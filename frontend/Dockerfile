# Stage build: Vite chỉ nạp biến VITE_* lúc build.
FROM node:22-alpine AS build
WORKDIR /app

# Mặc định dùng đường dẫn tương đối /api/v1: nginx proxy /api/ tới backend
# (network "backend", service "be") nên request đi đúng /api/v1/... của BE.
ARG VITE_API_BASE=/api/v1
ENV VITE_API_BASE=$VITE_API_BASE

COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

# Stage runtime: chỉ phục vụ dist/, không đưa source map hay node_modules vào image.
FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
