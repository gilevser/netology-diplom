# netology-diplom-backend

## Project setup

Собрать проект и установить зависимости с помощью Maven:
```
mvn clean package -DskipTests
```

## Запуск

Запустить контейнеры с помощью Docker Compose:
```
docker-compose up --build
```

## Пользователи

- В системе уже инициализирован пользователь с логином `admin` и паролем `q12345678`.
- Для регистрации нового пользователя можно отправить POST-запрос на:
  с телом запроса (JSON):

```json
{
    "login": "user",
    "password": "password"
}