FROM python:3.11-slim

WORKDIR /app
COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt

COPY train.py ./

ENV REDIS_URL=redis://redis:6379
CMD ["python", "train.py"]
