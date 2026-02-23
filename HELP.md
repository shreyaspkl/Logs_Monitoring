Quick Notes

Every backend change requires:

build JAR → commit → push → Render deploy


Without building a new JAR, Render will run the old compiled version.

You can test everything locally using the JAR before pushing:

java -jar target/<your-app>.jar


Render project link for all your services (including backend & frontend):
https://dashboard.render.com/project/prj-d40hgtfdiees739jhlj0

Render can be configured for autodeployment on a Git branch —
every push triggers an automatic deploy.

To connect PG DB through CMD-

psql "postgresql://logsdb_bca4_user:kLTjZswuOranEnl2SHrLeCcShCFvQ3FH@dpg-d44ele6mcj7s73e2qhag-a.singapore-postgres.render.com/logsdb_bca4"

i.e. 
psql "External Database URL from Render"

TIP:-
Whenever free tier of PG DB is getting expire, then just create a new free tier PG DB in render by suspending/deleting old DBs and then once created change the values in application.properties.

spring.datasource.url=jdbc:postgresql://{here paste the part after @ in External Database URL from Render }
spring.datasource.username=USERNAME_FROM_RENDER
spring.datasource.password=PASSWORD_HERE_FROM_RENDER

And lastly edit the below environment variable in render:-
SPRING_DATASOURCE_PASSWORD
SPRING_DATASOURCE_URL  - {jdbc:postgresql://dpg-d4ldenqli9vc73e7t160-a:5432/logsdb2}
SPRING_DATASOURCE_USERNAME
