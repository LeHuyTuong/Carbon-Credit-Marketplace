# Stage build 
FROM node:18 AS build
WORKDIR /app
COPY . .
RUN npm install && npm run build

# Stage runtime nginx
FROM nginx:1.25
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
