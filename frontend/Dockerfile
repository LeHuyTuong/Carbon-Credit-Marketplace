# Stage 1: build React
FROM node:18 AS build
WORKDIR /app
COPY . .
RUN npm install && npm run build

# Stage 2: serve bằng Nginx
FROM nginx:1.25
COPY --from=build /app/build /usr/share/nginx/html
EXPOSE 80
