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

# Runtime dynamic environment injection for React
RUN echo '#!/bin/sh' > /docker-entrypoint.d/30-env-config.sh && \
    echo 'echo "window._env_ = { REACT_APP_API_URL: \"${REACT_APP_API_URL:-}\" };" > /usr/share/nginx/html/env-config.js' >> /docker-entrypoint.d/30-env-config.sh && \
    chmod +x /docker-entrypoint.d/30-env-config.sh

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
