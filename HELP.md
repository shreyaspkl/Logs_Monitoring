Quick Notes

Every backend change requires:

build JAR → commit the jar also along with the code changes → push → Render deploy


Without building a new JAR and pushing it, Render will run the old compiled version. Becasue we are just copying the jar from target dir in the dockerFile

You can test everything locally using the JAR before pushing:

java -jar target/<your-app>.jar


Render project link for all your services (including backend & frontend):
https://dashboard.render.com/project/prj-d40hgtfdiees739jhlj0

Render can be configured for autodeployment on a Git branch —
every push triggers an automatic deploy.

To connect PG DB through CMD-

psql "postgresql://logs_pg_db_user:DUMMY@dpg-dummy-a.singapore-postgres.render.com/logs_pg_db"

i.e. 
psql "External Database URL from Render"

TIP:-
Whenever free tier of PG DB is getting expire, then just create a new free tier PG DB in render by suspending/deleting old DBs and then once created change the values in application.properties.

for local setup only/ no need to commit/push this fom application.properties
spring.datasource.url=jdbc:postgresql://{here paste the part after @ in External Database URL from Render }
spring.datasource.username=USERNAME_FROM_RENDER
spring.datasource.password=PASSWORD_HERE_FROM_RENDER

edit the below environment variable in render:-
SPRING_DATASOURCE_PASSWORD
SPRING_DATASOURCE_URL  - {jdbc:postgresql://dpg-DUMMY-a:5432/logsdb2} check this from connect externally/internally in pg db instance
SPRING_DATASOURCE_USERNAME


and if in frontend you dont see any logs entry then you need to manually insert rows into logs table using psql command given above 
