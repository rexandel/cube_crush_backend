#!/bin/bash

# Остановить скрипт при ошибке
set -e

echo "=== Начало развертывания Cube Crush Backend ==="

# 1. Обновление системы и установка зависимостей (Docker)
echo "--- Шаг 1: Проверка и установка Docker ---"

if ! command -v docker &> /dev/null; then
    echo "Docker не найден. Устанавливаем Docker..."
    sudo apt-get update
    sudo apt-get install -y ca-certificates curl gnupg
    sudo install -m 0755 -d /etc/apt/keyrings
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
    sudo chmod a+r /etc/apt/keyrings/docker.gpg

    echo \
      "deb [arch="$(dpkg --print-architecture)" signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
      "$(. /etc/os-release && echo "$VERSION_CODENAME")" stable" | \
      sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
    
    sudo apt-get update
    sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
    echo "Docker установлен."
else
    echo "Docker уже установлен."
fi

# Проверка прав доступа к Docker
if ! docker info > /dev/null 2>&1; then
    echo "Внимание: Текущий пользователь не имеет прав на выполнение команд Docker."
    echo "Попробуйте запустить скрипт с sudo или добавьте пользователя в группу docker."
    # Продолжаем, так как скрипт может быть запущен от root
fi

# 2. Настройка переменных окружения
echo "--- Шаг 2: Настройка конфигурации (.env) ---"
if [ ! -f .env ]; then
    echo "Файл .env не найден."
    if [ -f .env.example ]; then
        echo "Создаем .env из .env.example..."
        cp .env.example .env
        echo "ВНИМАНИЕ: Файл .env создан с настройками по умолчанию. Рекомендуется отредактировать его перед использованием в продакшене."
    else
        echo "ОШИБКА: Файл .env.example не найден. Пожалуйста, создайте файл .env вручную с необходимыми переменными (POSTGRES_DB, POSTGRES_USER, POSTGRES_PASSWORD, JWT_SECRET)."
        exit 1
    fi
else
    echo "Файл .env найден."
fi

# 3. Сборка и запуск контейнеров
echo "--- Шаг 3: Сборка и запуск контейнеров (включая скачивание библиотек) ---"
# Остановка старых контейнеров, если есть
sudo docker compose down || true

# Сборка и запуск
# --build заставит пересобрать образы, что вызовет скачивание зависимостей Maven внутри Dockerfile
echo "Запускаем docker compose up --build -d..."
sudo docker compose up --build -d

# 4. Ожидание запуска
echo "--- Шаг 4: Ожидание запуска сервисов (2 минуты) ---"
echo "Подождите, идет инициализация базы данных и сервисов..."
sleep 120

# 5. Проверка статуса
echo "--- Шаг 5: Проверка статуса ---"
sudo docker compose ps

echo ""
echo "Проверка логов Eureka Server (для уверенности, что он запустился):"
sudo docker compose logs --tail=20 eureka-server

echo ""
echo "=== Развертывание завершено ==="
echo "Если в выводе 'docker compose ps' все контейнеры имеют статус 'Up' (или 'healthy'), значит все прошло успешно."
