-- !Ups

create table specialization
(
    "abbrev" text PRIMARY KEY,
    "po"     text not null,
    "label"  text not null,
    FOREIGN KEY (po) REFERENCES po (abbrev)
);

alter table po_mandatory
    add column specialization text null;

alter table po_mandatory
    add constraint specialization_fk FOREIGN KEY (specialization) REFERENCES specialization (abbrev);

alter table po_optional
    add column specialization text null;

alter table po_optional
    add constraint specialization_fk FOREIGN KEY (specialization) REFERENCES specialization (abbrev);

-- !Downs

alter table po_optional
    drop constraint specialization_fk;

alter table po_optional
    drop column specialization;

alter table po_mandatory
    drop constraint specialization_fk;

alter table po_mandatory
    drop column specialization;

drop table specialization if exists;