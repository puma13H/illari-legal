FROM node:24-alpine AS build
WORKDIR /checkout
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY angular.json tsconfig.json ./
COPY src/ ./src/
RUN npm run build

FROM nginx:1.28-alpine
COPY --from=build /checkout/dist/portal/browser/ /usr/share/nginx/html/
COPY default.conf.template /etc/nginx/templates/default.conf.template
