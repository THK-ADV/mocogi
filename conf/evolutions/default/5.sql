-- !Ups

create table people_images
(
    "person"    text primary key,
    "image_url" text not null
);

alter table identity
    drop column image_url;
