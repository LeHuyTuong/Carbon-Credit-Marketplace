# Stage build 
FROM node:18 AS build
WORKDIR /app
# Vite chỉ nạp biến VITE_* lúc build. Không truyền VITE_API_BASE thì apiFetch rơi vào
# `import.meta.env.VITE_API_BASE` = undefined và mọi request chết ngay khi app chạy.
ARG VITE_API_BASE=/api
ENV VITE_API_BASE=$VITE_API_BASE
COPY . .
# Source maps chỉ dùng khi debug cục bộ: nếu bật, file .map của toàn bộ mã nguồn
# được nginx phục vụ công khai trong image production.
RUN npm install && npm run build -- --sourcemap false

# Stage runtime nginx
FROM nginx:1.25
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
