# Registers schema
 
# --- !Ups
 
create table load (
  id                        smallint not null auto_increment,
  timestamp                 timestamp,
  file                      varchar(255),
  label                     varchar(255),
  user                      varchar(255),
  rootRegId                 int,
  primary key (id)
);

 
create table register (
  id                        int not null auto_increment,
  name                      varchar(255) not null,
  baseAddr                  bigint not null,
  highAddr                  bigint,
  isRegset                  boolean not null,
  width                     smallint,
  isMap                     boolean,
  extRoot                   boolean,
  reps                      int,
  stride                    int,
  shortText                 varchar(255),
  longText                  clob,
  depth                     tinyint,
  loadId                    smallint,
  access                    varchar(255),
  alias                     boolean,
  donttest                  boolean,
  catCode                   int,
  parentPath                varchar(255),
  primary key (id)
);

create index idx_register_name_1 on register (name);
alter table register add constraint fk_load_register_1 foreign key (loadId) references load (id) on delete cascade;
alter table load add constraint fk_register_load_1 foreign key (rootRegId) references register (id);
 
create table descendant (
  id                        bigint not null auto_increment,
  parent                    int,
  descendant                int,
  primary key (id)
);

create index idx_descendant_descendant_1 on descendant (descendant);
create index idx_descendant_parent_1 on descendant (parent);
alter table descendant add constraint fk_register_descendant_1 foreign key (parent) references register (id) on delete cascade;
alter table descendant add constraint fk_register_descendant_2 foreign key (descendant) references register (id);
 
create table field (
  id                        bigint not null auto_increment,
  name                      varchar(255) not null,
  lowIdx                    smallint,
  width                     smallint,
  access                    varchar(255),
  rst                       varchar(255),
  hwmod                     boolean,
  counter                   boolean,
  intr                      boolean,
  shortText                 varchar(255),
  longText                  clob,
  dontcompare               boolean,
  subCatCode                int,
  regId                     int,
  primary key (id)
);

create index idx_field_regId_1 on field (regId);
alter table field add constraint fk_register_field_1 foreign key (regId) references register (id) on delete cascade;

# --- !Downs

SET REFERENTIAL_INTEGRITY FALSE;
drop table if exists load;
drop table if exists register;
drop table if exists descendant;
drop table if exists field;
SET REFERENTIAL_INTEGRITY TRUE;
