-- approvals for given id
select distinct on (qUser.role, qUser.study_program, qUser.module_draft) qUser.id,
                                                                         d.module,
                                                                         d.user_id,
                                                                         d.module_json,
                                                                         qUser.role,
                                                                         qUser.study_program,
                                                                         qUser.status,
                                                                         spp.person
from module_review as qUser
         left join (select *
                    from study_program_person
                    where person in (select id from person where campus_id = 'cnoss')) as spp
                   on qUser.study_program = spp.study_program and qUser.role = spp.role
         join (select *
               from module_review as qUser
                        join (select *
                              from study_program_person
                              where person in (select id from person where campus_id = 'cnoss')) as spp2
                             on qUser.study_program = spp2.study_program and qUser.role = spp2.role) as qAll
              on qUser.module_draft = qAll.module_draft
         join module_draft as d on qUser.module_draft = d.module

-- has pending approval request
select qUser.id,
       qUser.module_draft,
       qUser.role,
       qUser.study_program,
       qUser.status,
       spp.person
from module_review as qUser
         join (select *
                    from study_program_person
                    where person in (select id from person where campus_id = 'cnoss')) as spp
                   on qUser.study_program = spp.study_program and qUser.role = spp.role and qUser.status = 'pending' and qUser.id = '0cb17c43-ef26-4000-bf1c-2ca10cc5b603'