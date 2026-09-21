# Guía de Despliegue en AWS (Arquitectura de Producción)

## Arquitectura Objetivo
- **Capa de Cómputo**: AWS ECS Fargate (Serverless Containers).
- **Capa de Persistencia**: Amazon RDS (PostgreSQL 16).
- **Capa de Red**: VPC Privada con ALB (Application Load Balancer) en subred pública.

## Paso 1: Configurar Base de Datos (RDS)
1. Crear una base de datos **Amazon RDS PostgreSQL 16**.
2. Desplegar en **Subred Privada** (No accesible públicamente).
3. Habilitar Cifrado en Reposo (KMS).
4. Configurar Security Group para aceptar conexiones entrantes puerto 5432 solo desde el Security Group de ECS Fargate.

## Paso 2: Construir y Subir Imagen Docker (ECR)
1. Crear repositorio en **AWS ECR** (Elastic Container Registry).
2. Construir la imagen: `docker build -t case-backend .`
3. Hacer tag y push:
   ```bash
   aws ecr get-login-password --region <region> | docker login --username AWS --password-stdin <id>.dkr.ecr.<region>.amazonaws.com
   docker tag case-backend:latest <id>.dkr.ecr.<region>.amazonaws.com/case-backend:latest
   docker push <id>.dkr.ecr.<region>.amazonaws.com/case-backend:latest
   ```

## Paso 3: Configurar AWS ECS (Fargate)
1. Crear un **Cluster ECS**.
2. Crear un **Task Definition**:
   - Tipo de lanzamiento: FARGATE.
   - Rol de ejecución: `ecsTaskExecutionRole`.
   - Imagen: URL del repositorio ECR.
   - Asignar variables de entorno (`SPRING_PROFILES_ACTIVE=prod`, `SPRING_DATASOURCE_URL`, `JWT_SECRET`, etc.). En producción, es recomendable inyectarlas a través de AWS Secrets Manager.
   - Puerto del contenedor: 8080.
3. Crear un **Service**:
   - Tareas deseadas: Mínimo 2 para alta disponibilidad.
   - Asociar al Application Load Balancer (ALB).

## Paso 4: Configurar Load Balancer (ALB) y STOMP
1. Crear un **ALB** orientado a Internet (Subredes públicas).
2. Listener en puerto 443 (HTTPS) asociando un certificado de **AWS ACM**.
3. Target Group apuntando a las tareas ECS en puerto 8080.
4. **IMPORTANTE PARA WEBSOCKETS**: Asegurar que los Timeouts en el Load Balancer para conexiones idle superen el heartbeat de STOMP (ej. 300 segundos). Los WebSockets (ws:// / wss://) son soportados nativamente por AWS ALB.

## Paso 5: Almacenamiento (Amazon S3)
1. Crear bucket `case-collaborative-storage` con acceso bloqueado al público.
2. Otorgar permisos al rol de tarea de ECS (Task Role) para leer y escribir en este bucket usando la política `AmazonS3FullAccess` o políticas granulares según requerimiento para exportar/importar archivos .case o XMI.
