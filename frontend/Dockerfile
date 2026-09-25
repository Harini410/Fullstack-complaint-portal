# Multi-Stage Dockerfile for React Frontend
FROM node:20-alpine AS builder
WORKDIR /app

# Copy package descriptors and source files
COPY package*.json ./
COPY . .

# Use prebuilt bundle if present, otherwise install dependencies and compile
RUN if [ -f "./build/index.html" ]; then \
      echo "Prebuilt production bundle detected. Skipping rebuild."; \
    else \
      npm install && npm run build; \
    fi

# Stage 2: Minimal hardened Nginx web server
FROM nginx:alpine
COPY --from=builder /app/build /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
