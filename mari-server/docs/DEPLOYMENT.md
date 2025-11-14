# Mari Core Server Deployment Guide

## Prerequisites

- Node.js 16+
- MongoDB 4.4+
- Redis (optional, for caching)
- PM2 (for process management)

## Installation

1. Clone the repository:
```bash
git clone https://github.com/your-org/mari-core-server.git
cd mari-core-server
```

2. Install dependencies:
```bash
npm install
```

3. Configure environment variables:
```bash
cp .env.example .env
# Edit .env with your configuration
```

4. Start MongoDB and create database:
```bash
mongod --dbpath /var/lib/mongodb
```

5. Run database migrations (if any):
```bash
npm run migrate
```

6. Start the server:
```bash
npm start
```

## Production Deployment

### Using PM2
```bash
npm install -g pm2
pm2 start server.js --name mari-core
pm2 startup
pm2 save
```

### Using Docker
```dockerfile
FROM node:16-alpine
WORKDIR /app
COPY package*.json ./
RUN npm install --production
COPY . .
EXPOSE 3000
CMD ["node", "server.js"]
```

### Docker Compose
```yaml
version: '3.8'
services:
  app:
    build: .
    ports:
      - "3000:3000"
    environment:
      - DATABASE_URL=mongodb://mongo:27017/mari-core
    depends_on:
      - mongo
  mongo:
    image: mongo:4.4
    volumes:
      - mongo-data:/data/db
volumes:
  mongo-data:
```

## Monitoring

- Use PM2 monitoring: `pm2 monit`
- Set up health checks on `/health` endpoint
- Configure log rotation
- Set up alerting for critical errors

## Security

- Use HTTPS in production
- Configure firewall rules
- Set up rate limiting
- Use strong JWT secrets
- Enable MongoDB authentication
- Regular security updates
