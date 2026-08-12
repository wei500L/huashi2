-- LEXIBRIDGE_RESEARCH_V1 in-place content update
-- Scope: the questionnaire exactly mapped by releaseCode RES-AFC02D0823F2
-- (assessment_questionnaire_version id resolved below); no publish/release/version changes.
-- Idempotent: safe to re-run.
--
-- !! SUPERSEDED since 2026-08-12 — DO NOT RUN !!
-- This one-time migration was applied for the 2026-08-11 content update and assumed the
-- pre-restore field set (no BASIC-NAME / BASIC-CONTACT). The production V1 questionnaire
-- is now synchronized automatically on app-server startup by
-- LexiBridgeResearchSeedInitializer.updateResearchPackageContent(...) (restores the name /
-- contact fields, reorders profile items, applies the 60-minute duration and refreshes the
-- paper + publish snapshots). Re-running this script against the current database will fail:
-- the INSERT of BASIC-ENGLISH-MAJOR (below) collides with the existing row via
-- uk_assessment_questionnaire_item_code. Keep it only as a historical record.

START TRANSACTION;

SET @q_code = 'LEXIBRIDGE_RESEARCH_V1';
SET @bank_code = 'LEXIBRIDGE_SHARED';
SET @version_id = (SELECT v.id FROM assessment_questionnaire_version v
                   JOIN assessment_questionnaire q ON q.id = v.questionnaire_id
                   WHERE q.questionnaire_code = @q_code AND q.deleted = 0 AND v.deleted = 0
                   ORDER BY v.version_no DESC LIMIT 1);
SET @paper_id = (SELECT paper_id FROM assessment_questionnaire_version WHERE id = @version_id AND deleted = 0 LIMIT 1);
SET @bank_id = (SELECT id FROM assessment_question_bank WHERE bank_code = @bank_code AND deleted = 0 LIMIT 1);

-- section BASIC_INFO
UPDATE assessment_questionnaire_section
SET title = '基本信息',
    description = '',
    shared_material = '',
    scored_item_count = 0
WHERE questionnaire_version_id = @version_id AND section_code = 'BASIC_INFO' AND deleted = 0;

-- section P1A
UPDATE assessment_questionnaire_section
SET title = 'Partie 1 – Compréhension lexicale 词汇理解 / Section A',
    description = '请选出下列法语单词对应的正确中文含义',
    shared_material = '',
    scored_item_count = 10
WHERE questionnaire_version_id = @version_id AND section_code = 'P1A' AND deleted = 0;

-- section P1B
UPDATE assessment_questionnaire_section
SET title = 'Partie 1 – Compréhension lexicale 词汇理解 / Section B',
    description = '请选出与下列短语中划线单词意思相同的选项；第 6–10 题请选出与下列短语中划线单词意思相反的选项。',
    shared_material = '',
    scored_item_count = 10
WHERE questionnaire_version_id = @version_id AND section_code = 'P1B' AND deleted = 0;

-- section P2
UPDATE assessment_questionnaire_section
SET title = 'Partie 2 – Compréhension en contexte 语境理解',
    description = '请根据句子选择画线单词的同义解释',
    shared_material = '',
    scored_item_count = 10
WHERE questionnaire_version_id = @version_id AND section_code = 'P2' AND deleted = 0;

-- section P3
UPDATE assessment_questionnaire_section
SET title = 'Partie 3 – Texte à trous',
    description = '请阅读下面的完整短文，为每个空格选择最佳答案。',
    shared_material = 'Une journée de travail
Chaque matin, Julien prend toujours le même (1) ______ pour se rendre à son bureau, situé en périphérie（市郊） de la ville. Il ne veut absolument pas (2) ______ son train de 8h15, car une réunion très importante l’attend dès son arrivée. Après avoir quitté son appartement, il enfile rapidement son (3) ______ préféré, une veste bleue qu’il a achetée la semaine dernière, puis il sort sans prendre le temps de déjeuner.
Dans l’entreprise où il travaille depuis trois ans, son (4) ______ lui plaît énormément, même si les journées sont parfois chargées. Chaque matin, il doit (5) ______ à de nombreux courriels que ses clients lui envoient la veille. Il apprécie particulièrement la (6) ______ qu’il a avec ses collègues de bureau, bien que l’ambiance puisse devenir tendue en fin de semaine.
La semaine dernière, son supérieur hiérarchique（上级） lui a demandé de (7) ______ à une longue réunion qui a duré plus de deux heures. Julien a accepté cette demande sans hésiter une seule seconde. Cependant, les décisions qui ont été prises lors de cette réunion ont commencé à le (8) ______, car aucune de ses idées n’a finalement été retenue par l’équipe. C’est une (9) ______ que tout le monde peut vivre ce genre de déception au cours de sa carrière professionnelle. Malgré tout, cela reste une situation relativement (10) ______ dans la vie de bureau, et Julien garde espoir que les choses s’amélioreront demain.',
    scored_item_count = 10
WHERE questionnaire_version_id = @version_id AND section_code = 'P3' AND deleted = 0;

-- section P4T1
UPDATE assessment_questionnaire_section
SET title = 'Partie 4 – Compréhension écrite / Texte 1',
    description = '请阅读下面的完整短文，选择最佳答案。',
    shared_material = 'La pollution est un problème important aujourd''hui. Bref, notre planète a besoin de solutions rapides. Si nous regardons vers l''horizon, nous voyons souvent les fumées grises provenant des usines. Certaines personnes sont sensibles à la qualité de l''air : elles se mettent à tousser dès que celle-ci se dégrade. Mais beaucoup de gens ignorent que la pollution peut aussi nuire aux animaux de nombreuses façons : un éventuel incendie pourrait détruire leurs habitats, et les produits chimiques rejetés dans la nature peuvent empoisonner leur nourriture.
Heureusement, des gestes simples peuvent aider. Par exemple, prendre les transports en commun plutôt que sa voiture réduit la pollution. Si chacun fait un petit effort, les conséquences seront moins graves pour la nature. Agir maintenant est essentiel pour l''avenir.',
    scored_item_count = 5
WHERE questionnaire_version_id = @version_id AND section_code = 'P4T1' AND deleted = 0;

-- section P4T2
UPDATE assessment_questionnaire_section
SET title = 'Partie 4 – Compréhension écrite / Texte 2',
    description = '请阅读下面的完整短文，选择最佳答案。',
    shared_material = 'L''année dernière, Sophie a quitté son poste dans une entreprise internationale. Elle travaillait comme secrétaire depuis cinq ans, mais elle voulait réaliser son rêve : ouvrir une librairie indépendante. Ce rêve lui est venu quand elle était au collège. Actuellement, elle suit une formation de gestion commerciale et elle doit souvent passer des examens pendant ses études.
C''est un peu difficile car elle doit étudier beaucoup de matières nouvelles, mais elle trouve cette expérience très stimulante. Son magasin ouvrira le mois prochain, et elle espère que ce sera un succès.',
    scored_item_count = 5
WHERE questionnaire_version_id = @version_id AND section_code = 'P4T2' AND deleted = 0;

-- section P4T3
UPDATE assessment_questionnaire_section
SET title = 'Partie 4 – Compréhension écrite / Texte 3',
    description = '请阅读下面的法语短文，根据短文内容判断下列句子的正误。正确的请写“V”，错误的请写“F”，判断为F的题目需要在横线上写明错误原因。',
    shared_material = 'Au cœur du Vaucluse, Avignon attire chaque année des milliers de visiteurs. Ce matin-là, Léa et son petit ami se promenaient dans les ruelles pavées. Tous deux profitaient de leurs dernières heures avec une pointe de nostalgie : ils avaient déjà réservé leurs billets de train pour le soir.
Le soleil du Midi s’est mis à taper très fort. La chaleur était étouffante. Mais ils voulaient absolument visiter la vieille église du centre. Les ruelles étaient toutes pareilles, elles tournaient sans cesse. La carte ne servait à rien. Léa commençait à s’énerver. Son ami lui a pris doucement la main et lui a dit : « Ne t’inquiète pas, on va bien la trouver. »
Tout à coup, ils ont entendu un bruit d’eau. En le suivant, ils ont débouché sur une petite place. Là, une vieille fontaine coulait, claire et fraîche. Beaucoup de monde se tenait autour : des enfants trempaient leur casquette, des touristes remplissaient leur bouteille. Léa et son ami se sont mis de l’eau sur le visage et les bras, et ils se sont sentis tout de suite mieux.
Pourtant, l’église restait introuvable. Le jeune homme a alors aperçu un passant qui se reposait au bord de la fontaine. Il s’est approché et lui a demandé la direction. L’homme a souri et a montré du doigt une ruelle discrète : « Passez par là, tournez au coin, et vous tomberez juste devant. »
Au tournant suivant, l’église est apparue. Ils ont poussé la lourde porte en bois et sont entrés. À l’intérieur, tout était frais et silencieux. Une lumière douce tombait sur une statue de la Vierge. Sa figure douce les a beaucoup touchés.
Ils sont restés longtemps côte à côte, sans rien dire. À ce moment, ils se sentaient pleinement engagés dans la contemplation. Tout leur paraissait formidable : la paix autour d’eux, ce silence, les gens sympathiques. Ils ont oublié la chaleur, la fatigue, le train du soir. Il ne restait qu’une émotion simple et profonde.',
    scored_item_count = 10
WHERE questionnaire_version_id = @version_id AND section_code = 'P4T3' AND deleted = 0;

-- BASIC-INSTRUCTION
UPDATE assessment_question q
JOIN assessment_questionnaire_item i ON i.assessment_question_id = q.id
SET q.question_type = 'INSTRUCTION',
    q.stem_text = '【亲爱的同学：
您好！欢迎参与本次法语词汇与阅读理解能力测试！本测试结果仅用于学术研究，所有数据严格保密，不会用作其他用途。请您认真阅读每一道题目，结合文本内容选出最佳答案。答题过程中请勿查阅词典、相互交流，独立完成作答。整套测试答题时长约 40 分钟，请合理安排时间。
感谢您的配合与支持！】',
    q.prompt_text = '',
    q.options_json = '[]',
    q.correct_answer_json = '[]',
    q.explanation_text = '',
    q.score = 0,
    q.section_code = 'BASIC_INFO',
    q.required_answer = false,
    q.weight = 1,
    q.transfer_category = '',
    q.context_level = '',
    q.construct_code = '',
    q.target_word = '',
    q.option_explanations_json = '{}',
    q.display_condition_json = 'null'
WHERE i.questionnaire_version_id = @version_id AND i.item_code = 'BASIC-INSTRUCTION' AND i.deleted = 0 AND q.deleted = 0;
UPDATE assessment_questionnaire_item
SET required_answer = false, scored = false, weight = 1,
    transfer_category = '',
    context_level = '',
    construct_code = '',
    target_word = '',
    option_explanations_json = '{}',
    display_condition_json = 'null'
WHERE questionnaire_version_id = @version_id AND item_code = 'BASIC-INSTRUCTION' AND deleted = 0;
UPDATE assessment_question_version
SET question_type = 'INSTRUCTION',
    stem_text = '【亲爱的同学：
您好！欢迎参与本次法语词汇与阅读理解能力测试！本测试结果仅用于学术研究，所有数据严格保密，不会用作其他用途。请您认真阅读每一道题目，结合文本内容选出最佳答案。答题过程中请勿查阅词典、相互交流，独立完成作答。整套测试答题时长约 40 分钟，请合理安排时间。
感谢您的配合与支持！】',
    prompt_text = '',
    options_json = '[]',
    correct_answer_json = '[]',
    explanation_text = '',
    option_explanations_json = '{}',
    required_answer = false, weight = 1,
    transfer_category = '',
    context_level = '',
    construct_code = '',
    target_word = '',
    display_condition_json = 'null',
    source_reference = 'questionnaire:paragraph-59',
    content_hash = '9388daf4c61e5b0ccdfb9191ef7fedb0551f06d05bfd7d36db39efbd38598d25'
WHERE question_bank_id = @bank_id AND question_code = 'BASIC-INSTRUCTION' AND version_no = 1 AND deleted = 0;

-- BASIC-NAME
UPDATE assessment_question q
JOIN assessment_questionnaire_item i ON i.assessment_question_id = q.id
SET q.question_type = 'SHORT_TEXT',
    q.stem_text = '您的姓名：',
    q.prompt_text = '',
    q.options_json = '[]',
    q.correct_answer_json = '[]',
    q.explanation_text = '',
    q.score = 0,
    q.section_code = 'BASIC_INFO',
    q.required_answer = true,
    q.weight = 1,
    q.transfer_category = '',
    q.context_level = '',
    q.construct_code = '',
    q.target_word = '',
    q.option_explanations_json = '{}',
    q.display_condition_json = 'null'
WHERE i.questionnaire_version_id = @version_id AND i.item_code = 'BASIC-NAME' AND i.deleted = 0 AND q.deleted = 0;
UPDATE assessment_questionnaire_item
SET required_answer = true, scored = false, weight = 1,
    transfer_category = '',
    context_level = '',
    construct_code = '',
    target_word = '',
    option_explanations_json = '{}',
    display_condition_json = 'null'
WHERE questionnaire_version_id = @version_id AND item_code = 'BASIC-NAME' AND deleted = 0;
UPDATE assessment_question_version
SET question_type = 'SHORT_TEXT',
    stem_text = '您的姓名：',
    prompt_text = '',
    options_json = '[]',
    correct_answer_json = '[]',
    explanation_text = '',
    option_explanations_json = '{}',
    required_answer = true, weight = 1,
    transfer_category = '',
    context_level = '',
    construct_code = '',
    target_word = '',
    display_condition_json = 'null',
    source_reference = 'questionnaire:paragraph-60',
    content_hash = 'bf1d7688aec09e57f4e90dfc8128b01b87e9b35bce415306641f788d7586f167'
WHERE question_bank_id = @bank_id AND question_code = 'BASIC-NAME' AND version_no = 1 AND deleted = 0;

-- BASIC-CONTACT
UPDATE assessment_question q
JOIN assessment_questionnaire_item i ON i.assessment_question_id = q.id
SET q.question_type = 'SHORT_TEXT',
    q.stem_text = '您的联系方式是（电话/QQ/……）：',
    q.prompt_text = '',
    q.options_json = '[]',
    q.correct_answer_json = '[]',
    q.explanation_text = '',
    q.score = 0,
    q.section_code = 'BASIC_INFO',
    q.required_answer = false,
    q.weight = 1,
    q.transfer_category = '',
    q.context_level = '',
    q.construct_code = '',
    q.target_word = '',
    q.option_explanations_json = '{}',
    q.display_condition_json = 'null'
WHERE i.questionnaire_version_id = @version_id AND i.item_code = 'BASIC-CONTACT' AND i.deleted = 0 AND q.deleted = 0;
UPDATE assessment_questionnaire_item
SET required_answer = false, scored = false, weight = 1,
    transfer_category = '',
    context_level = '',
    construct_code = '',
    target_word = '',
    option_explanations_json = '{}',
    display_condition_json = 'null'
WHERE questionnaire_version_id = @version_id AND item_code = 'BASIC-CONTACT' AND deleted = 0;
UPDATE assessment_question_version
SET question_type = 'SHORT_TEXT',
    stem_text = '您的联系方式是（电话/QQ/……）：',
    prompt_text = '',
    options_json = '[]',
    correct_answer_json = '[]',
    explanation_text = '',
    option_explanations_json = '{}',
    required_answer = false, weight = 1,
    transfer_category = '',
    context_level = '',
    construct_code = '',
    target_word = '',
    display_condition_json = 'null',
    source_reference = 'questionnaire:paragraph-61',
    content_hash = 'b301d5062bf98cc7a0ed3e193a6dcc57c58c1ccd066f6f294d6b2fbbd58e7970'
WHERE question_bank_id = @bank_id AND question_code = 'BASIC-CONTACT' AND version_no = 1 AND deleted = 0;

-- BASIC-STATUS
UPDATE assessment_question q
JOIN assessment_questionnaire_item i ON i.assessment_question_id = q.id
SET q.question_type = 'SINGLE_CHOICE',
    q.stem_text = '您目前是：',
    q.prompt_text = '',
    q.options_json = '[{"key":"FRENCH_MAJOR","label":"法语专业学生"},{"key":"FRENCH_SECOND_LANGUAGE","label":"法语作为二外的学生"},{"key":"NON_MAJOR","label":"非专业法语学习者（自学/其他）"}]',
    q.correct_answer_json = '[]',
    q.explanation_text = '',
    q.score = 0,
    q.section_code = 'BASIC_INFO',
    q.required_answer = false,
    q.weight = 1,
    q.transfer_category = '',
    q.context_level = '',
    q.construct_code = '',
    q.target_word = '',
    q.option_explanations_json = '{}',
    q.display_condition_json = 'null'
WHERE i.questionnaire_version_id = @version_id AND i.item_code = 'BASIC-STATUS' AND i.deleted = 0 AND q.deleted = 0;
UPDATE assessment_questionnaire_item
SET required_answer = false, scored = false, weight = 1,
    transfer_category = '',
    context_level = '',
    construct_code = '',
    target_word = '',
    option_explanations_json = '{}',
    display_condition_json = 'null'
WHERE questionnaire_version_id = @version_id AND item_code = 'BASIC-STATUS' AND deleted = 0;
UPDATE assessment_question_version
SET question_type = 'SINGLE_CHOICE',
    stem_text = '您目前是：',
    prompt_text = '',
    options_json = '[{"key":"FRENCH_MAJOR","label":"法语专业学生"},{"key":"FRENCH_SECOND_LANGUAGE","label":"法语作为二外的学生"},{"key":"NON_MAJOR","label":"非专业法语学习者（自学/其他）"}]',
    correct_answer_json = '[]',
    explanation_text = '',
    option_explanations_json = '{}',
    required_answer = false, weight = 1,
    transfer_category = '',
    context_level = '',
    construct_code = '',
    target_word = '',
    display_condition_json = 'null',
    source_reference = 'questionnaire:paragraph-62',
    content_hash = '4dd12be5c380b5889b4b02fe50403d724943b839f97317c1f26e2df627e0452e'
WHERE question_bank_id = @bank_id AND question_code = 'BASIC-STATUS' AND version_no = 1 AND deleted = 0;

-- BASIC-GAOKAO-ENGLISH
UPDATE assessment_question q
JOIN assessment_questionnaire_item i ON i.assessment_question_id = q.id
SET q.question_type = 'NUMBER',
    q.stem_text = '您的英语学习水平为：高考英语分数______',
    q.prompt_text = '',
    q.options_json = '[]',
    q.correct_answer_json = '[]',
    q.explanation_text = '',
    q.score = 0,
    q.section_code = 'BASIC_INFO',
    q.required_answer = false,
    q.weight = 1,
    q.transfer_category = '',
    q.context_level = '',
    q.construct_code = '',
    q.target_word = '',
    q.option_explanations_json = '{}',
    q.display_condition_json = 'null'
WHERE i.questionnaire_version_id = @version_id AND i.item_code = 'BASIC-GAOKAO-ENGLISH' AND i.deleted = 0 AND q.deleted = 0;
UPDATE assessment_questionnaire_item
SET required_answer = false, scored = false, weight = 1,
    transfer_category = '',
    context_level = '',
    construct_code = '',
    target_word = '',
    option_explanations_json = '{}',
    display_condition_json = 'null'
WHERE questionnaire_version_id = @version_id AND item_code = 'BASIC-GAOKAO-ENGLISH' AND deleted = 0;
UPDATE assessment_question_version
SET question_type = 'NUMBER',
    stem_text = '您的英语学习水平为：高考英语分数______',
    prompt_text = '',
    options_json = '[]',
    correct_answer_json = '[]',
    explanation_text = '',
    option_explanations_json = '{}',
    required_answer = false, weight = 1,
    transfer_category = '',
    context_level = '',
    construct_code = '',
    target_word = '',
    display_condition_json = 'null',
    source_reference = 'questionnaire:paragraph-63',
    content_hash = '3d1322826538599a20c2f757b88e9d8b50b621131d753917e4ee8306b4b248a1'
WHERE question_bank_id = @bank_id AND question_code = 'BASIC-GAOKAO-ENGLISH' AND version_no = 1 AND deleted = 0;

-- new item BASIC-ENGLISH-MAJOR (insert at flow position 6)
INSERT INTO assessment_question_version
    (question_bank_id,question_code,version_no,question_type,stem_text,prompt_text,options_json,
     correct_answer_json,explanation_text,option_explanations_json,required_answer,weight,
     transfer_category,context_level,construct_code,target_word,display_condition_json,source_reference,
     content_hash,created_by,updated_by)
VALUES (@bank_id,'BASIC-ENGLISH-MAJOR',1,'SINGLE_CHOICE',
     '您是否为英语专业学生：','请选择英语专业或非英语专业。','[{"key":"ENGLISH_MAJOR","label":"英语专业"},{"key":"NON_ENGLISH_MAJOR","label":"非英语专业"}]',
     '[]','','{}',
     true,1,NULL,NULL,NULL,'',
     'null','questionnaire:fix-2026-08-basic-english-major',
     '50576559d90bdb9bd0e6303c57d589735d17f3678898f3154748295850d2abee',0,0);
SET @qvid = LAST_INSERT_ID();
UPDATE assessment_question SET sort_order = sort_order + 1
WHERE paper_id = @paper_id AND sort_order >= 6 AND deleted = 0;
INSERT INTO assessment_question
    (paper_id,question_type,sort_order,stem_text,prompt_text,options_json,correct_answer_json,
     explanation_text,score,question_version_id,section_code,required_answer,weight,transfer_category,
     context_level,construct_code,target_word,option_explanations_json,display_condition_json,created_by,updated_by)
VALUES (@paper_id,'SINGLE_CHOICE',6,
     '您是否为英语专业学生：','请选择英语专业或非英语专业。','[{"key":"ENGLISH_MAJOR","label":"英语专业"},{"key":"NON_ENGLISH_MAJOR","label":"非英语专业"}]',
     '[]','',0,@qvid,'BASIC_INFO',
     true,1,NULL,NULL,NULL,'',
     '{}',
     'null',0,0);
SET @qid = LAST_INSERT_ID();
INSERT INTO assessment_questionnaire_item
    (questionnaire_version_id,section_id,assessment_question_id,question_version_id,item_code,
     required_answer,scored,weight,transfer_category,context_level,construct_code,target_word,
     option_explanations_json,display_condition_json,created_by,updated_by)
VALUES (@version_id,
     (SELECT id FROM assessment_questionnaire_section WHERE questionnaire_version_id = @version_id AND section_code = 'BASIC_INFO' AND deleted = 0 LIMIT 1),
     @qid,@qvid,'BASIC-ENGLISH-MAJOR',true,false,1,NULL,NULL,NULL,
     '','{}',
     'null',0,0);

-- BASIC-CET4
UPDATE assessment_question q
JOIN assessment_questionnaire_item i ON i.assessment_question_id = q.id
SET q.question_type = 'NUMBER',
    q.stem_text = '□ 四级分数_____',
    q.prompt_text = '',
    q.options_json = '[]',
    q.correct_answer_json = '[]',
    q.explanation_text = '',
    q.score = 0,
    q.section_code = 'BASIC_INFO',
    q.required_answer = false,
    q.weight = 1,
    q.transfer_category = '',
    q.context_level = '',
    q.construct_code = '',
    q.target_word = '',
    q.option_explanations_json = '{}',
    q.display_condition_json = 'null'
WHERE i.questionnaire_version_id = @version_id AND i.item_code = 'BASIC-CET4' AND i.deleted = 0 AND q.deleted = 0;
UPDATE assessment_questionnaire_item
SET required_answer = false, scored = false, weight = 1,
    transfer_category = '',
    context_level = '',
    construct_code = '',
    target_word = '',
    option_explanations_json = '{}',
    display_condition_json = 'null'
WHERE questionnaire_version_id = @version_id AND item_code = 'BASIC-CET4' AND deleted = 0;
UPDATE assessment_question_version
SET question_type = 'NUMBER',
    stem_text = '□ 四级分数_____',
    prompt_text = '',
    options_json = '[]',
    correct_answer_json = '[]',
    explanation_text = '',
    option_explanations_json = '{}',
    required_answer = false, weight = 1,
    transfer_category = '',
    context_level = '',
    construct_code = '',
    target_word = '',
    display_condition_json = 'null',
    source_reference = 'questionnaire:paragraph-64',
    content_hash = '8be463c23dcd018e7b902cfd84283184a57eeef9a9c087e33fb14894ebbd5ede'
WHERE question_bank_id = @bank_id AND question_code = 'BASIC-CET4' AND version_no = 1 AND deleted = 0;

-- BASIC-CET6
UPDATE assessment_question q
JOIN assessment_questionnaire_item i ON i.assessment_question_id = q.id
SET q.question_type = 'NUMBER',
    q.stem_text = '□ 六级分数_____',
    q.prompt_text = '',
    q.options_json = '[]',
    q.correct_answer_json = '[]',
    q.explanation_text = '',
    q.score = 0,
    q.section_code = 'BASIC_INFO',
    q.required_answer = false,
    q.weight = 1,
    q.transfer_category = '',
    q.context_level = '',
    q.construct_code = '',
    q.target_word = '',
    q.option_explanations_json = '{}',
    q.display_condition_json = 'null'
WHERE i.questionnaire_version_id = @version_id AND i.item_code = 'BASIC-CET6' AND i.deleted = 0 AND q.deleted = 0;
UPDATE assessment_questionnaire_item
SET required_answer = false, scored = false, weight = 1,
    transfer_category = '',
    context_level = '',
    construct_code = '',
    target_word = '',
    option_explanations_json = '{}',
    display_condition_json = 'null'
WHERE questionnaire_version_id = @version_id AND item_code = 'BASIC-CET6' AND deleted = 0;
UPDATE assessment_question_version
SET question_type = 'NUMBER',
    stem_text = '□ 六级分数_____',
    prompt_text = '',
    options_json = '[]',
    correct_answer_json = '[]',
    explanation_text = '',
    option_explanations_json = '{}',
    required_answer = false, weight = 1,
    transfer_category = '',
    context_level = '',
    construct_code = '',
    target_word = '',
    display_condition_json = 'null',
    source_reference = 'questionnaire:paragraph-65',
    content_hash = '4299e70381472dc35f73abb714b5ab8c4c4757f0ebbd6a1b66b8d00825f782e1'
WHERE question_bank_id = @bank_id AND question_code = 'BASIC-CET6' AND version_no = 1 AND deleted = 0;

-- BASIC-TEM4
UPDATE assessment_question q
JOIN assessment_questionnaire_item i ON i.assessment_question_id = q.id
SET q.question_type = 'NUMBER',
    q.stem_text = '□ 专四分数_____',
    q.prompt_text = '',
    q.options_json = '[]',
    q.correct_answer_json = '[]',
    q.explanation_text = '',
    q.score = 0,
    q.section_code = 'BASIC_INFO',
    q.required_answer = false,
    q.weight = 1,
    q.transfer_category = '',
    q.context_level = '',
    q.construct_code = '',
    q.target_word = '',
    q.option_explanations_json = '{}',
    q.display_condition_json = '{"fieldCode":"BASIC-ENGLISH-MAJOR","operator":"EQ","value":"ENGLISH_MAJOR"}'
WHERE i.questionnaire_version_id = @version_id AND i.item_code = 'BASIC-TEM4' AND i.deleted = 0 AND q.deleted = 0;
UPDATE assessment_questionnaire_item
SET required_answer = false, scored = false, weight = 1,
    transfer_category = '',
    context_level = '',
    construct_code = '',
    target_word = '',
    option_explanations_json = '{}',
    display_condition_json = '{"fieldCode":"BASIC-ENGLISH-MAJOR","operator":"EQ","value":"ENGLISH_MAJOR"}'
WHERE questionnaire_version_id = @version_id AND item_code = 'BASIC-TEM4' AND deleted = 0;
UPDATE assessment_question_version
SET question_type = 'NUMBER',
    stem_text = '□ 专四分数_____',
    prompt_text = '',
    options_json = '[]',
    correct_answer_json = '[]',
    explanation_text = '',
    option_explanations_json = '{}',
    required_answer = false, weight = 1,
    transfer_category = '',
    context_level = '',
    construct_code = '',
    target_word = '',
    display_condition_json = '{"fieldCode":"BASIC-ENGLISH-MAJOR","operator":"EQ","value":"ENGLISH_MAJOR"}',
    source_reference = 'questionnaire:paragraph-66',
    content_hash = '04b68af201331288c55034b572af77ed324c852307c793c581811324378ce764'
WHERE question_bank_id = @bank_id AND question_code = 'BASIC-TEM4' AND version_no = 1 AND deleted = 0;

-- BASIC-TEM8
UPDATE assessment_question q
JOIN assessment_questionnaire_item i ON i.assessment_question_id = q.id
SET q.question_type = 'NUMBER',
    q.stem_text = '□ 专八分数_____',
    q.prompt_text = '',
    q.options_json = '[]',
    q.correct_answer_json = '[]',
    q.explanation_text = '',
    q.score = 0,
    q.section_code = 'BASIC_INFO',
    q.required_answer = false,
    q.weight = 1,
    q.transfer_category = '',
    q.context_level = '',
    q.construct_code = '',
    q.target_word = '',
    q.option_explanations_json = '{}',
    q.display_condition_json = '{"fieldCode":"BASIC-ENGLISH-MAJOR","operator":"EQ","value":"ENGLISH_MAJOR"}'
WHERE i.questionnaire_version_id = @version_id AND i.item_code = 'BASIC-TEM8' AND i.deleted = 0 AND q.deleted = 0;
UPDATE assessment_questionnaire_item
SET required_answer = false, scored = false, weight = 1,
    transfer_category = '',
    context_level = '',
    construct_code = '',
    target_word = '',
    option_explanations_json = '{}',
    display_condition_json = '{"fieldCode":"BASIC-ENGLISH-MAJOR","operator":"EQ","value":"ENGLISH_MAJOR"}'
WHERE questionnaire_version_id = @version_id AND item_code = 'BASIC-TEM8' AND deleted = 0;
UPDATE assessment_question_version
SET question_type = 'NUMBER',
    stem_text = '□ 专八分数_____',
    prompt_text = '',
    options_json = '[]',
    correct_answer_json = '[]',
    explanation_text = '',
    option_explanations_json = '{}',
    required_answer = false, weight = 1,
    transfer_category = '',
    context_level = '',
    construct_code = '',
    target_word = '',
    display_condition_json = '{"fieldCode":"BASIC-ENGLISH-MAJOR","operator":"EQ","value":"ENGLISH_MAJOR"}',
    source_reference = 'questionnaire:paragraph-67',
    content_hash = '81e95369f9c30082e00c2bc1767186ce270c0ba3a02a30cb5db85ced40e754e4'
WHERE question_bank_id = @bank_id AND question_code = 'BASIC-TEM8' AND version_no = 1 AND deleted = 0;

-- BASIC-FRENCH-DURATION
UPDATE assessment_question q
JOIN assessment_questionnaire_item i ON i.assessment_question_id = q.id
SET q.question_type = 'SINGLE_CHOICE',
    q.stem_text = '您的法语学习时长约为：',
    q.prompt_text = '',
    q.options_json = '[{"key":"DURATION_1","label":"间断地在学习，未系统学习过"},{"key":"DURATION_2","label":"少于6个月"},{"key":"DURATION_3","label":"6个月~1年"},{"key":"DURATION_4","label":"1~2年"},{"key":"DURATION_5","label":"2年以上"}]',
    q.correct_answer_json = '[]',
    q.explanation_text = '',
    q.score = 0,
    q.section_code = 'BASIC_INFO',
    q.required_answer = false,
    q.weight = 1,
    q.transfer_category = '',
    q.context_level = '',
    q.construct_code = '',
    q.target_word = '',
    q.option_explanations_json = '{}',
    q.display_condition_json = 'null'
WHERE i.questionnaire_version_id = @version_id AND i.item_code = 'BASIC-FRENCH-DURATION' AND i.deleted = 0 AND q.deleted = 0;
UPDATE assessment_questionnaire_item
SET required_answer = false, scored = false, weight = 1,
    transfer_category = '',
    context_level = '',
    construct_code = '',
    target_word = '',
    option_explanations_json = '{}',
    display_condition_json = 'null'
WHERE questionnaire_version_id = @version_id AND item_code = 'BASIC-FRENCH-DURATION' AND deleted = 0;
UPDATE assessment_question_version
SET question_type = 'SINGLE_CHOICE',
    stem_text = '您的法语学习时长约为：',
    prompt_text = '',
    options_json = '[{"key":"DURATION_1","label":"间断地在学习，未系统学习过"},{"key":"DURATION_2","label":"少于6个月"},{"key":"DURATION_3","label":"6个月~1年"},{"key":"DURATION_4","label":"1~2年"},{"key":"DURATION_5","label":"2年以上"}]',
    correct_answer_json = '[]',
    explanation_text = '',
    option_explanations_json = '{}',
    required_answer = false, weight = 1,
    transfer_category = '',
    context_level = '',
    construct_code = '',
    target_word = '',
    display_condition_json = 'null',
    source_reference = 'questionnaire:paragraph-68',
    content_hash = '69e533429268da3c85baf64815133b32e45bb05d3a5b59b558cc63d51f3bda6d'
WHERE question_bank_id = @bank_id AND question_code = 'BASIC-FRENCH-DURATION' AND version_no = 1 AND deleted = 0;

-- BASIC-OTHER-LANGUAGE
UPDATE assessment_question q
JOIN assessment_questionnaire_item i ON i.assessment_question_id = q.id
SET q.question_type = 'SHORT_TEXT',
    q.stem_text = '是否接触过其他语种（西语 / 意语 / 葡语/……）？',
    q.prompt_text = '',
    q.options_json = '[]',
    q.correct_answer_json = '[]',
    q.explanation_text = '',
    q.score = 0,
    q.section_code = 'BASIC_INFO',
    q.required_answer = false,
    q.weight = 1,
    q.transfer_category = '',
    q.context_level = '',
    q.construct_code = '',
    q.target_word = '',
    q.option_explanations_json = '{}',
    q.display_condition_json = 'null'
WHERE i.questionnaire_version_id = @version_id AND i.item_code = 'BASIC-OTHER-LANGUAGE' AND i.deleted = 0 AND q.deleted = 0;
UPDATE assessment_questionnaire_item
SET required_answer = false, scored = false, weight = 1,
    transfer_category = '',
    context_level = '',
    construct_code = '',
    target_word = '',
    option_explanations_json = '{}',
    display_condition_json = 'null'
WHERE questionnaire_version_id = @version_id AND item_code = 'BASIC-OTHER-LANGUAGE' AND deleted = 0;
UPDATE assessment_question_version
SET question_type = 'SHORT_TEXT',
    stem_text = '是否接触过其他语种（西语 / 意语 / 葡语/……）？',
    prompt_text = '',
    options_json = '[]',
    correct_answer_json = '[]',
    explanation_text = '',
    option_explanations_json = '{}',
    required_answer = false, weight = 1,
    transfer_category = '',
    context_level = '',
    construct_code = '',
    target_word = '',
    display_condition_json = 'null',
    source_reference = 'questionnaire:paragraph-69',
    content_hash = '23770f06521a7338cbf72da7c5d3b378d13ec9d373925928cd7ce48cc067117c'
WHERE question_bank_id = @bank_id AND question_code = 'BASIC-OTHER-LANGUAGE' AND version_no = 1 AND deleted = 0;

-- P1A-01
UPDATE assessment_question q
JOIN assessment_questionnaire_item i ON i.assessment_question_id = q.id
SET q.question_type = 'SINGLE_CHOICE',
    q.stem_text = 'description',
    q.prompt_text = '请选出下列法语单词对应的正确中文含义',
    q.options_json = '[{"key":"A","label":"描述"},{"key":"B","label":"订阅"},{"key":"C","label":"处方"},{"key":"D","label":"抄写"}]',
    q.correct_answer_json = '["A"]',
    q.explanation_text = '1. description
#题目分析：答案：A
此题为英法同源词。法语 description = “描述”，同源英语 description = “描述”。
干扰项 B “订阅”（subscription）：英语中与 description 共享 -scription 词根（写），学生易因拼写相似而误选。
干扰项 C“处方”（prescription）：同样含 -scription，且英语 prescription 与 description 形近。
干扰项 D “抄写”（transcription）：同样含 -scription，词形高度相似，语义域相近（书写类）。',
    q.score = 1,
    q.section_code = 'P1A',
    q.required_answer = true,
    q.weight = 1,
    q.transfer_category = 'COGNATE',
    q.context_level = 'WORD',
    q.construct_code = 'LEXICAL_TRANSFER',
    q.target_word = 'description',
    q.option_explanations_json = '{"B":"干扰项 B “订阅”（subscription）：英语中与 description 共享 -scription 词根（写），学生易因拼写相似而误选。","C":"干扰项 C“处方”（prescription）：同样含 -scription，且英语 prescription 与 description 形近。","D":"干扰项 D “抄写”（transcription）：同样含 -scription，词形高度相似，语义域相近（书写类）。","A":"此题为英法同源词。法语 description = “描述”，同源英语 description = “描述”。"}',
    q.display_condition_json = 'null'
WHERE i.questionnaire_version_id = @version_id AND i.item_code = 'P1A-01' AND i.deleted = 0 AND q.deleted = 0;
UPDATE assessment_questionnaire_item
SET required_answer = true, scored = true, weight = 1,
    transfer_category = 'COGNATE',
    context_level = 'WORD',
    construct_code = 'LEXICAL_TRANSFER',
    target_word = 'description',
    option_explanations_json = '{"B":"干扰项 B “订阅”（subscription）：英语中与 description 共享 -scription 词根（写），学生易因拼写相似而误选。","C":"干扰项 C“处方”（prescription）：同样含 -scription，且英语 prescription 与 description 形近。","D":"干扰项 D “抄写”（transcription）：同样含 -scription，词形高度相似，语义域相近（书写类）。","A":"此题为英法同源词。法语 description = “描述”，同源英语 description = “描述”。"}',
    display_condition_json = 'null'
WHERE questionnaire_version_id = @version_id AND item_code = 'P1A-01' AND deleted = 0;
UPDATE assessment_question_version
SET question_type = 'SINGLE_CHOICE',
    stem_text = 'description',
    prompt_text = '请选出下列法语单词对应的正确中文含义',
    options_json = '[{"key":"A","label":"描述"},{"key":"B","label":"订阅"},{"key":"C","label":"处方"},{"key":"D","label":"抄写"}]',
    correct_answer_json = '["A"]',
    explanation_text = '1. description
#题目分析：答案：A
此题为英法同源词。法语 description = “描述”，同源英语 description = “描述”。
干扰项 B “订阅”（subscription）：英语中与 description 共享 -scription 词根（写），学生易因拼写相似而误选。
干扰项 C“处方”（prescription）：同样含 -scription，且英语 prescription 与 description 形近。
干扰项 D “抄写”（transcription）：同样含 -scription，词形高度相似，语义域相近（书写类）。',
    option_explanations_json = '{"B":"干扰项 B “订阅”（subscription）：英语中与 description 共享 -scription 词根（写），学生易因拼写相似而误选。","C":"干扰项 C“处方”（prescription）：同样含 -scription，且英语 prescription 与 description 形近。","D":"干扰项 D “抄写”（transcription）：同样含 -scription，词形高度相似，语义域相近（书写类）。","A":"此题为英法同源词。法语 description = “描述”，同源英语 description = “描述”。"}',
    required_answer = true, weight = 1,
    transfer_category = 'COGNATE',
    context_level = 'WORD',
    construct_code = 'LEXICAL_TRANSFER',
    target_word = 'description',
    display_condition_json = 'null',
    source_reference = 'questionnaire:P1A-01',
    content_hash = '2984f61050c6592a5f03af03fd3b5774cab434d73190a2551630c693749cfa77'
WHERE question_bank_id = @bank_id AND question_code = 'P1A-01' AND version_no = 1 AND deleted = 0;

-- P1A-02
UPDATE assessment_question q
JOIN assessment_questionnaire_item i ON i.assessment_question_id = q.id
SET q.question_type = 'SINGLE_CHOICE',
    q.stem_text = 'actuellement',
    q.prompt_text = '请选出下列法语单词对应的正确中文含义',
    q.options_json = '[{"key":"A","label":"实际上"},{"key":"B","label":"目前"},{"key":"C","label":"最终"},{"key":"D","label":"可能"}]',
    q.correct_answer_json = '["B"]',
    q.explanation_text = '2. actuellement
#题目分析：答案：B
此题为假朋友（同形异义词）。法语 actuellement = “目前，当前”，英语形似词 actually = “实际上，事实上”。
干扰项 A “实际上”：英语 actually 的直接翻译，是典型的负迁移来源。
干扰项 C “最终”：与英语 eventually 混淆，拼写不同但语义容易联想。
干扰项 D “可能”：与英语 possibly 混淆，作为泛化干扰项。
（1、2题为一组）',
    q.score = 1,
    q.section_code = 'P1A',
    q.required_answer = true,
    q.weight = 1,
    q.transfer_category = 'FALSE_FRIEND',
    q.context_level = 'WORD',
    q.construct_code = 'LEXICAL_TRANSFER',
    q.target_word = 'actuellement',
    q.option_explanations_json = '{"A":"干扰项 A “实际上”：英语 actually 的直接翻译，是典型的负迁移来源。","C":"干扰项 C “最终”：与英语 eventually 混淆，拼写不同但语义容易联想。","D":"干扰项 D “可能”：与英语 possibly 混淆，作为泛化干扰项。","B":"此题为假朋友（同形异义词）。法语 actuellement = “目前，当前”，英语形似词 actually = “实际上，事实上”。\\n（1、2题为一组）"}',
    q.display_condition_json = 'null'
WHERE i.questionnaire_version_id = @version_id AND i.item_code = 'P1A-02' AND i.deleted = 0 AND q.deleted = 0;
UPDATE assessment_questionnaire_item
SET required_answer = true, scored = true, weight = 1,
    transfer_category = 'FALSE_FRIEND',
    context_level = 'WORD',
    construct_code = 'LEXICAL_TRANSFER',
    target_word = 'actuellement',
    option_explanations_json = '{"A":"干扰项 A “实际上”：英语 actually 的直接翻译，是典型的负迁移来源。","C":"干扰项 C “最终”：与英语 eventually 混淆，拼写不同但语义容易联想。","D":"干扰项 D “可能”：与英语 possibly 混淆，作为泛化干扰项。","B":"此题为假朋友（同形异义词）。法语 actuellement = “目前，当前”，英语形似词 actually = “实际上，事实上”。\\n（1、2题为一组）"}',
    display_condition_json = 'null'
WHERE questionnaire_version_id = @version_id AND item_code = 'P1A-02' AND deleted = 0;
UPDATE assessment_question_version
SET question_type = 'SINGLE_CHOICE',
    stem_text = 'actuellement',
    prompt_text = '请选出下列法语单词对应的正确中文含义',
    options_json = '[{"key":"A","label":"实际上"},{"key":"B","label":"目前"},{"key":"C","label":"最终"},{"key":"D","label":"可能"}]',
    correct_answer_json = '["B"]',
    explanation_text = '2. actuellement
#题目分析：答案：B
此题为假朋友（同形异义词）。法语 actuellement = “目前，当前”，英语形似词 actually = “实际上，事实上”。
干扰项 A “实际上”：英语 actually 的直接翻译，是典型的负迁移来源。
干扰项 C “最终”：与英语 eventually 混淆，拼写不同但语义容易联想。
干扰项 D “可能”：与英语 possibly 混淆，作为泛化干扰项。
（1、2题为一组）',
    option_explanations_json = '{"A":"干扰项 A “实际上”：英语 actually 的直接翻译，是典型的负迁移来源。","C":"干扰项 C “最终”：与英语 eventually 混淆，拼写不同但语义容易联想。","D":"干扰项 D “可能”：与英语 possibly 混淆，作为泛化干扰项。","B":"此题为假朋友（同形异义词）。法语 actuellement = “目前，当前”，英语形似词 actually = “实际上，事实上”。\\n（1、2题为一组）"}',
    required_answer = true, weight = 1,
    transfer_category = 'FALSE_FRIEND',
    context_level = 'WORD',
    construct_code = 'LEXICAL_TRANSFER',
    target_word = 'actuellement',
    display_condition_json = 'null',
    source_reference = 'questionnaire:P1A-02',
    content_hash = 'bed2d358591b4e06f2bddd19c6d402407c90a05fe5d7013c62fbb7d2595ca817'
WHERE question_bank_id = @bank_id AND question_code = 'P1A-02' AND version_no = 1 AND deleted = 0;

-- P1A-03
UPDATE assessment_question q
JOIN assessment_questionnaire_item i ON i.assessment_question_id = q.id
SET q.question_type = 'SINGLE_CHOICE',
    q.stem_text = 'librairie',
    q.prompt_text = '请选出下列法语单词对应的正确中文含义',
    q.options_json = '[{"key":"A","label":"图书馆"},{"key":"B","label":"文具店"},{"key":"C","label":"书店"},{"key":"D","label":"出版社"}]',
    q.correct_answer_json = '["C"]',
    q.explanation_text = '3. librairie
#题目分析：答案：C
此题为假朋友（同形异义词）。法语 librairie = “书店”，英语形似词 library = “图书馆”。
干扰项 A “图书馆”：英语 library 的直接翻译，是负迁移的主要来源。
干扰项 B “文具店”：语义场与书店相近（售卖文化用品），且法语中有 papeterie 一词，易混淆。
干扰项 D “出版社”：与书店同为出版相关语义域，但词义不同，用于检测语义精细度。',
    q.score = 1,
    q.section_code = 'P1A',
    q.required_answer = true,
    q.weight = 1,
    q.transfer_category = 'FALSE_FRIEND',
    q.context_level = 'WORD',
    q.construct_code = 'LEXICAL_TRANSFER',
    q.target_word = 'librairie',
    q.option_explanations_json = '{"A":"干扰项 A “图书馆”：英语 library 的直接翻译，是负迁移的主要来源。","B":"干扰项 B “文具店”：语义场与书店相近（售卖文化用品），且法语中有 papeterie 一词，易混淆。","D":"干扰项 D “出版社”：与书店同为出版相关语义域，但词义不同，用于检测语义精细度。","C":"此题为假朋友（同形异义词）。法语 librairie = “书店”，英语形似词 library = “图书馆”。"}',
    q.display_condition_json = 'null'
WHERE i.questionnaire_version_id = @version_id AND i.item_code = 'P1A-03' AND i.deleted = 0 AND q.deleted = 0;
UPDATE assessment_questionnaire_item
SET required_answer = true, scored = true, weight = 1,
    transfer_category = 'FALSE_FRIEND',
    context_level = 'WORD',
    construct_code = 'LEXICAL_TRANSFER',
    target_word = 'librairie',
    option_explanations_json = '{"A":"干扰项 A “图书馆”：英语 library 的直接翻译，是负迁移的主要来源。","B":"干扰项 B “文具店”：语义场与书店相近（售卖文化用品），且法语中有 papeterie 一词，易混淆。","D":"干扰项 D “出版社”：与书店同为出版相关语义域，但词义不同，用于检测语义精细度。","C":"此题为假朋友（同形异义词）。法语 librairie = “书店”，英语形似词 library = “图书馆”。"}',
    display_condition_json = 'null'
WHERE questionnaire_version_id = @version_id AND item_code = 'P1A-03' AND deleted = 0;
UPDATE assessment_question_version
SET question_type = 'SINGLE_CHOICE',
    stem_text = 'librairie',
    prompt_text = '请选出下列法语单词对应的正确中文含义',
    options_json = '[{"key":"A","label":"图书馆"},{"key":"B","label":"文具店"},{"key":"C","label":"书店"},{"key":"D","label":"出版社"}]',
    correct_answer_json = '["C"]',
    explanation_text = '3. librairie
#题目分析：答案：C
此题为假朋友（同形异义词）。法语 librairie = “书店”，英语形似词 library = “图书馆”。
干扰项 A “图书馆”：英语 library 的直接翻译，是负迁移的主要来源。
干扰项 B “文具店”：语义场与书店相近（售卖文化用品），且法语中有 papeterie 一词，易混淆。
干扰项 D “出版社”：与书店同为出版相关语义域，但词义不同，用于检测语义精细度。',
    option_explanations_json = '{"A":"干扰项 A “图书馆”：英语 library 的直接翻译，是负迁移的主要来源。","B":"干扰项 B “文具店”：语义场与书店相近（售卖文化用品），且法语中有 papeterie 一词，易混淆。","D":"干扰项 D “出版社”：与书店同为出版相关语义域，但词义不同，用于检测语义精细度。","C":"此题为假朋友（同形异义词）。法语 librairie = “书店”，英语形似词 library = “图书馆”。"}',
    required_answer = true, weight = 1,
    transfer_category = 'FALSE_FRIEND',
    context_level = 'WORD',
    construct_code = 'LEXICAL_TRANSFER',
    target_word = 'librairie',
    display_condition_json = 'null',
    source_reference = 'questionnaire:P1A-03',
    content_hash = 'a608501ba752640c8704527f15d7a805f5873ccac1ecd0882deeaf1a3518890f'
WHERE question_bank_id = @bank_id AND question_code = 'P1A-03' AND version_no = 1 AND deleted = 0;

-- P1A-04
UPDATE assessment_question q
JOIN assessment_questionnaire_item i ON i.assessment_question_id = q.id
SET q.question_type = 'SINGLE_CHOICE',
    q.stem_text = 'bavarder',
    q.prompt_text = '请选出下列法语单词对应的正确中文含义',
    q.options_json = '[{"key":"A","label":"闲聊"},{"key":"B","label":"保存"},{"key":"C","label":"守卫"},{"key":"D","label":"辩论"}]',
    q.correct_answer_json = '["A"]',
    q.explanation_text = '4. bavarder
#题目分析：答案：A
此题为纯法语对照词。法语 bavarder = “闲聊”，英语无直接同源词。
干扰项 B “保存”：与法语 sauvegarder 形似，但与 bavarder 无关。
干扰项 C “守卫”：与法语 garder 相关，语音部分相似。
干扰项 D “辩论”：语义上与“说话”同属言语行为域，但程度不同，用于检测语义区分能力。',
    q.score = 1,
    q.section_code = 'P1A',
    q.required_answer = true,
    q.weight = 1,
    q.transfer_category = 'COGNATE',
    q.context_level = 'WORD',
    q.construct_code = 'LEXICAL_TRANSFER',
    q.target_word = 'bavarder',
    q.option_explanations_json = '{"B":"干扰项 B “保存”：与法语 sauvegarder 形似，但与 bavarder 无关。","C":"干扰项 C “守卫”：与法语 garder 相关，语音部分相似。","D":"干扰项 D “辩论”：语义上与“说话”同属言语行为域，但程度不同，用于检测语义区分能力。","A":"此题为纯法语对照词。法语 bavarder = “闲聊”，英语无直接同源词。"}',
    q.display_condition_json = 'null'
WHERE i.questionnaire_version_id = @version_id AND i.item_code = 'P1A-04' AND i.deleted = 0 AND q.deleted = 0;
UPDATE assessment_questionnaire_item
SET required_answer = true, scored = true, weight = 1,
    transfer_category = 'COGNATE',
    context_level = 'WORD',
    construct_code = 'LEXICAL_TRANSFER',
    target_word = 'bavarder',
    option_explanations_json = '{"B":"干扰项 B “保存”：与法语 sauvegarder 形似，但与 bavarder 无关。","C":"干扰项 C “守卫”：与法语 garder 相关，语音部分相似。","D":"干扰项 D “辩论”：语义上与“说话”同属言语行为域，但程度不同，用于检测语义区分能力。","A":"此题为纯法语对照词。法语 bavarder = “闲聊”，英语无直接同源词。"}',
    display_condition_json = 'null'
WHERE questionnaire_version_id = @version_id AND item_code = 'P1A-04' AND deleted = 0;
UPDATE assessment_question_version
SET question_type = 'SINGLE_CHOICE',
    stem_text = 'bavarder',
    prompt_text = '请选出下列法语单词对应的正确中文含义',
    options_json = '[{"key":"A","label":"闲聊"},{"key":"B","label":"保存"},{"key":"C","label":"守卫"},{"key":"D","label":"辩论"}]',
    correct_answer_json = '["A"]',
    explanation_text = '4. bavarder
#题目分析：答案：A
此题为纯法语对照词。法语 bavarder = “闲聊”，英语无直接同源词。
干扰项 B “保存”：与法语 sauvegarder 形似，但与 bavarder 无关。
干扰项 C “守卫”：与法语 garder 相关，语音部分相似。
干扰项 D “辩论”：语义上与“说话”同属言语行为域，但程度不同，用于检测语义区分能力。',
    option_explanations_json = '{"B":"干扰项 B “保存”：与法语 sauvegarder 形似，但与 bavarder 无关。","C":"干扰项 C “守卫”：与法语 garder 相关，语音部分相似。","D":"干扰项 D “辩论”：语义上与“说话”同属言语行为域，但程度不同，用于检测语义区分能力。","A":"此题为纯法语对照词。法语 bavarder = “闲聊”，英语无直接同源词。"}',
    required_answer = true, weight = 1,
    transfer_category = 'COGNATE',
    context_level = 'WORD',
    construct_code = 'LEXICAL_TRANSFER',
    target_word = 'bavarder',
    display_condition_json = 'null',
    source_reference = 'questionnaire:P1A-04',
    content_hash = 'f70595a3840f530f32dcbf624da93cc5ca3592f8e296f8008b948880c22f2b86'
WHERE question_bank_id = @bank_id AND question_code = 'P1A-04' AND version_no = 1 AND deleted = 0;

-- P1A-05
UPDATE assessment_question q
JOIN assessment_questionnaire_item i ON i.assessment_question_id = q.id
SET q.question_type = 'SINGLE_CHOICE',
    q.stem_text = 'hésiter',
    q.prompt_text = '请选出下列法语单词对应的正确中文含义',
    q.options_json = '[{"key":"A","label":"栖息"},{"key":"B","label":"坚持"},{"key":"C","label":"展示"},{"key":"D","label":"犹豫"}]',
    q.correct_answer_json = '["D"]',
    q.explanation_text = '5.hésiter
#题目分析：答案：D
此题为英法同源词。法语 hésiter = “犹豫”，同源英语 hesitate = “犹豫”。
干扰项 A “居住”：与法语 habiter（居住）形近。
干扰项 B “坚持”：与法语 persister / insister 语义相关，且英语 hesitate 的反义概念，用于检测语义反向联想。
干扰项 C “附着”：与法语 adhérer（附着）形近。
（3、4、5题为一组）',
    q.score = 1,
    q.section_code = 'P1A',
    q.required_answer = true,
    q.weight = 1,
    q.transfer_category = 'COGNATE',
    q.context_level = 'WORD',
    q.construct_code = 'LEXICAL_TRANSFER',
    q.target_word = 'hésiter',
    q.option_explanations_json = '{"A":"干扰项 A “居住”：与法语 habiter（居住）形近。","B":"干扰项 B “坚持”：与法语 persister / insister 语义相关，且英语 hesitate 的反义概念，用于检测语义反向联想。","C":"干扰项 C “附着”：与法语 adhérer（附着）形近。","D":"此题为英法同源词。法语 hésiter = “犹豫”，同源英语 hesitate = “犹豫”。\\n（3、4、5题为一组）"}',
    q.display_condition_json = 'null'
WHERE i.questionnaire_version_id = @version_id AND i.item_code = 'P1A-05' AND i.deleted = 0 AND q.deleted = 0;
UPDATE assessment_questionnaire_item
SET required_answer = true, scored = true, weight = 1,
    transfer_category = 'COGNATE',
    context_level = 'WORD',
    construct_code = 'LEXICAL_TRANSFER',
    target_word = 'hésiter',
    option_explanations_json = '{"A":"干扰项 A “居住”：与法语 habiter（居住）形近。","B":"干扰项 B “坚持”：与法语 persister / insister 语义相关，且英语 hesitate 的反义概念，用于检测语义反向联想。","C":"干扰项 C “附着”：与法语 adhérer（附着）形近。","D":"此题为英法同源词。法语 hésiter = “犹豫”，同源英语 hesitate = “犹豫”。\\n（3、4、5题为一组）"}',
    display_condition_json = 'null'
WHERE questionnaire_version_id = @version_id AND item_code = 'P1A-05' AND deleted = 0;
UPDATE assessment_question_version
SET question_type = 'SINGLE_CHOICE',
    stem_text = 'hésiter',
    prompt_text = '请选出下列法语单词对应的正确中文含义',
    options_json = '[{"key":"A","label":"栖息"},{"key":"B","label":"坚持"},{"key":"C","label":"展示"},{"key":"D","label":"犹豫"}]',
    correct_answer_json = '["D"]',
    explanation_text = '5.hésiter
#题目分析：答案：D
此题为英法同源词。法语 hésiter = “犹豫”，同源英语 hesitate = “犹豫”。
干扰项 A “居住”：与法语 habiter（居住）形近。
干扰项 B “坚持”：与法语 persister / insister 语义相关，且英语 hesitate 的反义概念，用于检测语义反向联想。
干扰项 C “附着”：与法语 adhérer（附着）形近。
（3、4、5题为一组）',
    option_explanations_json = '{"A":"干扰项 A “居住”：与法语 habiter（居住）形近。","B":"干扰项 B “坚持”：与法语 persister / insister 语义相关，且英语 hesitate 的反义概念，用于检测语义反向联想。","C":"干扰项 C “附着”：与法语 adhérer（附着）形近。","D":"此题为英法同源词。法语 hésiter = “犹豫”，同源英语 hesitate = “犹豫”。\\n（3、4、5题为一组）"}',
    required_answer = true, weight = 1,
    transfer_category = 'COGNATE',
    context_level = 'WORD',
    construct_code = 'LEXICAL_TRANSFER',
    target_word = 'hésiter',
    display_condition_json = 'null',
    source_reference = 'questionnaire:P1A-05',
    content_hash = 'db245b08101f7e8a7ddd1092f03ac53a86ab4de179b7a588652064f53835491e'
WHERE question_bank_id = @bank_id AND question_code = 'P1A-05' AND version_no = 1 AND deleted = 0;

-- P1A-06
UPDATE assessment_question q
JOIN assessment_questionnaire_item i ON i.assessment_question_id = q.id
SET q.question_type = 'SINGLE_CHOICE',
    q.stem_text = 'participer',
    q.prompt_text = '请选出下列法语单词对应的正确中文含义',
    q.options_json = '[{"key":"A","label":"参加"},{"key":"B","label":"分享"},{"key":"C","label":"合作"},{"key":"D","label":"游行"}]',
    q.correct_answer_json = '["A"]',
    q.explanation_text = '6. participer
#题目分析：答案：A
此题为英法同源词。法语 participer = “参加”，同源英语 participate = “参加”。
干扰项 B “分享”：法语 partager 与 participer 词根相同（part-），语义相关但不同。
干扰项 C “合作”：语义域相近，易混淆。
干扰项 D “游行”：英语 parade，词形部分相似（par-开头），用于检测注意力。',
    q.score = 1,
    q.section_code = 'P1A',
    q.required_answer = true,
    q.weight = 1,
    q.transfer_category = 'COGNATE',
    q.context_level = 'WORD',
    q.construct_code = 'LEXICAL_TRANSFER',
    q.target_word = 'participer',
    q.option_explanations_json = '{"B":"干扰项 B “分享”：法语 partager 与 participer 词根相同（part-），语义相关但不同。","C":"干扰项 C “合作”：语义域相近，易混淆。","D":"干扰项 D “游行”：英语 parade，词形部分相似（par-开头），用于检测注意力。","A":"此题为英法同源词。法语 participer = “参加”，同源英语 participate = “参加”。"}',
    q.display_condition_json = 'null'
WHERE i.questionnaire_version_id = @version_id AND i.item_code = 'P1A-06' AND i.deleted = 0 AND q.deleted = 0;
UPDATE assessment_questionnaire_item
SET required_answer = true, scored = true, weight = 1,
    transfer_category = 'COGNATE',
    context_level = 'WORD',
    construct_code = 'LEXICAL_TRANSFER',
    target_word = 'participer',
    option_explanations_json = '{"B":"干扰项 B “分享”：法语 partager 与 participer 词根相同（part-），语义相关但不同。","C":"干扰项 C “合作”：语义域相近，易混淆。","D":"干扰项 D “游行”：英语 parade，词形部分相似（par-开头），用于检测注意力。","A":"此题为英法同源词。法语 participer = “参加”，同源英语 participate = “参加”。"}',
    display_condition_json = 'null'
WHERE questionnaire_version_id = @version_id AND item_code = 'P1A-06' AND deleted = 0;
UPDATE assessment_question_version
SET question_type = 'SINGLE_CHOICE',
    stem_text = 'participer',
    prompt_text = '请选出下列法语单词对应的正确中文含义',
    options_json = '[{"key":"A","label":"参加"},{"key":"B","label":"分享"},{"key":"C","label":"合作"},{"key":"D","label":"游行"}]',
    correct_answer_json = '["A"]',
    explanation_text = '6. participer
#题目分析：答案：A
此题为英法同源词。法语 participer = “参加”，同源英语 participate = “参加”。
干扰项 B “分享”：法语 partager 与 participer 词根相同（part-），语义相关但不同。
干扰项 C “合作”：语义域相近，易混淆。
干扰项 D “游行”：英语 parade，词形部分相似（par-开头），用于检测注意力。',
    option_explanations_json = '{"B":"干扰项 B “分享”：法语 partager 与 participer 词根相同（part-），语义相关但不同。","C":"干扰项 C “合作”：语义域相近，易混淆。","D":"干扰项 D “游行”：英语 parade，词形部分相似（par-开头），用于检测注意力。","A":"此题为英法同源词。法语 participer = “参加”，同源英语 participate = “参加”。"}',
    required_answer = true, weight = 1,
    transfer_category = 'COGNATE',
    context_level = 'WORD',
    construct_code = 'LEXICAL_TRANSFER',
    target_word = 'participer',
    display_condition_json = 'null',
    source_reference = 'questionnaire:P1A-06',
    content_hash = '08e91c00dc4af8d6c5fe4c0903ce8d6c2d38de311cc44fd6abb24876baa8bae7'
WHERE question_bank_id = @bank_id AND question_code = 'P1A-06' AND version_no = 1 AND deleted = 0;

-- P1A-07
UPDATE assessment_question q
JOIN assessment_questionnaire_item i ON i.assessment_question_id = q.id
SET q.question_type = 'SINGLE_CHOICE',
    q.stem_text = 'prétendre',
    q.prompt_text = '请选出下列法语单词对应的正确中文含义',
    q.options_json = '[{"key":"A","label":"声称"},{"key":"B","label":"预料"},{"key":"C","label":"假装"},{"key":"D","label":"保护"}]',
    q.correct_answer_json = '["A"]',
    q.explanation_text = '7. prétendre
#题目分析：答案：A
此题为假朋友（同形异义词）。法语 prétendre = “声称”，英语形似词 pretend = “假装”；但英语 pretend 更侧重“虚假表演”。
干扰项 B “预料”：英语 predict，无词形相似，但学生可能因 pré- 前缀（表“预先”）而误猜“预料”。
干扰项 C “假装”：英语 pretend的直接翻译，是负迁移的主要来源。
干扰项 D “保护”：英语 protect，法语 protéger，词形部分相似（prot- / prét-），语义无关，用于检测前缀误判。
（6、7题为一组对应）',
    q.score = 1,
    q.section_code = 'P1A',
    q.required_answer = true,
    q.weight = 1,
    q.transfer_category = 'FALSE_FRIEND',
    q.context_level = 'WORD',
    q.construct_code = 'LEXICAL_TRANSFER',
    q.target_word = 'prétendre',
    q.option_explanations_json = '{"B":"干扰项 B “预料”：英语 predict，无词形相似，但学生可能因 pré- 前缀（表“预先”）而误猜“预料”。","C":"干扰项 C “假装”：英语 pretend的直接翻译，是负迁移的主要来源。","D":"干扰项 D “保护”：英语 protect，法语 protéger，词形部分相似（prot- / prét-），语义无关，用于检测前缀误判。","A":"此题为假朋友（同形异义词）。法语 prétendre = “声称”，英语形似词 pretend = “假装”；但英语 pretend 更侧重“虚假表演”。\\n（6、7题为一组对应）"}',
    q.display_condition_json = 'null'
WHERE i.questionnaire_version_id = @version_id AND i.item_code = 'P1A-07' AND i.deleted = 0 AND q.deleted = 0;
UPDATE assessment_questionnaire_item
SET required_answer = true, scored = true, weight = 1,
    transfer_category = 'FALSE_FRIEND',
    context_level = 'WORD',
    construct_code = 'LEXICAL_TRANSFER',
    target_word = 'prétendre',
    option_explanations_json = '{"B":"干扰项 B “预料”：英语 predict，无词形相似，但学生可能因 pré- 前缀（表“预先”）而误猜“预料”。","C":"干扰项 C “假装”：英语 pretend的直接翻译，是负迁移的主要来源。","D":"干扰项 D “保护”：英语 protect，法语 protéger，词形部分相似（prot- / prét-），语义无关，用于检测前缀误判。","A":"此题为假朋友（同形异义词）。法语 prétendre = “声称”，英语形似词 pretend = “假装”；但英语 pretend 更侧重“虚假表演”。\\n（6、7题为一组对应）"}',
    display_condition_json = 'null'
WHERE questionnaire_version_id = @version_id AND item_code = 'P1A-07' AND deleted = 0;
UPDATE assessment_question_version
SET question_type = 'SINGLE_CHOICE',
    stem_text = 'prétendre',
    prompt_text = '请选出下列法语单词对应的正确中文含义',
    options_json = '[{"key":"A","label":"声称"},{"key":"B","label":"预料"},{"key":"C","label":"假装"},{"key":"D","label":"保护"}]',
    correct_answer_json = '["A"]',
    explanation_text = '7. prétendre
#题目分析：答案：A
此题为假朋友（同形异义词）。法语 prétendre = “声称”，英语形似词 pretend = “假装”；但英语 pretend 更侧重“虚假表演”。
干扰项 B “预料”：英语 predict，无词形相似，但学生可能因 pré- 前缀（表“预先”）而误猜“预料”。
干扰项 C “假装”：英语 pretend的直接翻译，是负迁移的主要来源。
干扰项 D “保护”：英语 protect，法语 protéger，词形部分相似（prot- / prét-），语义无关，用于检测前缀误判。
（6、7题为一组对应）',
    option_explanations_json = '{"B":"干扰项 B “预料”：英语 predict，无词形相似，但学生可能因 pré- 前缀（表“预先”）而误猜“预料”。","C":"干扰项 C “假装”：英语 pretend的直接翻译，是负迁移的主要来源。","D":"干扰项 D “保护”：英语 protect，法语 protéger，词形部分相似（prot- / prét-），语义无关，用于检测前缀误判。","A":"此题为假朋友（同形异义词）。法语 prétendre = “声称”，英语形似词 pretend = “假装”；但英语 pretend 更侧重“虚假表演”。\\n（6、7题为一组对应）"}',
    required_answer = true, weight = 1,
    transfer_category = 'FALSE_FRIEND',
    context_level = 'WORD',
    construct_code = 'LEXICAL_TRANSFER',
    target_word = 'prétendre',
    display_condition_json = 'null',
    source_reference = 'questionnaire:P1A-07',
    content_hash = 'ac08e43d07acddfc8f5bbd2e440252c0b224259c989e42272afc4f51eb815251'
WHERE question_bank_id = @bank_id AND question_code = 'P1A-07' AND version_no = 1 AND deleted = 0;

-- P1A-08
UPDATE assessment_question q
JOIN assessment_questionnaire_item i ON i.assessment_question_id = q.id
SET q.question_type = 'SINGLE_CHOICE',
    q.stem_text = 'injurier',
    q.prompt_text = '请选出下列法语单词对应的正确中文含义',
    q.options_json = '[{"key":"A","label":"侮辱"},{"key":"B","label":"伤害"},{"key":"C","label":"命令"},{"key":"D","label":"注射"}]',
    q.correct_answer_json = '["A"]',
    q.explanation_text = '8. injurier
#题目分析：答案：A
此题为假朋友（同形异义词）。法语 injurier = “辱骂 / 侮辱”，英语形似词 injure = “伤害”。
干扰项 B “伤害”：英语 injure 的直接翻译，是强干扰项（负迁移）。
干扰项 C “命令”：作为中性填充。
干扰项 D “注射”：英语 inject，法语 injecter，词形前缀 in- + j 开头，与 injurier 部分相似，易混淆。',
    q.score = 1,
    q.section_code = 'P1A',
    q.required_answer = true,
    q.weight = 1,
    q.transfer_category = 'FALSE_FRIEND',
    q.context_level = 'WORD',
    q.construct_code = 'LEXICAL_TRANSFER',
    q.target_word = 'injurier',
    q.option_explanations_json = '{"B":"干扰项 B “伤害”：英语 injure 的直接翻译，是强干扰项（负迁移）。","C":"干扰项 C “命令”：作为中性填充。","D":"干扰项 D “注射”：英语 inject，法语 injecter，词形前缀 in- + j 开头，与 injurier 部分相似，易混淆。","A":"此题为假朋友（同形异义词）。法语 injurier = “辱骂 / 侮辱”，英语形似词 injure = “伤害”。"}',
    q.display_condition_json = 'null'
WHERE i.questionnaire_version_id = @version_id AND i.item_code = 'P1A-08' AND i.deleted = 0 AND q.deleted = 0;
UPDATE assessment_questionnaire_item
SET required_answer = true, scored = true, weight = 1,
    transfer_category = 'FALSE_FRIEND',
    context_level = 'WORD',
    construct_code = 'LEXICAL_TRANSFER',
    target_word = 'injurier',
    option_explanations_json = '{"B":"干扰项 B “伤害”：英语 injure 的直接翻译，是强干扰项（负迁移）。","C":"干扰项 C “命令”：作为中性填充。","D":"干扰项 D “注射”：英语 inject，法语 injecter，词形前缀 in- + j 开头，与 injurier 部分相似，易混淆。","A":"此题为假朋友（同形异义词）。法语 injurier = “辱骂 / 侮辱”，英语形似词 injure = “伤害”。"}',
    display_condition_json = 'null'
WHERE questionnaire_version_id = @version_id AND item_code = 'P1A-08' AND deleted = 0;
UPDATE assessment_question_version
SET question_type = 'SINGLE_CHOICE',
    stem_text = 'injurier',
    prompt_text = '请选出下列法语单词对应的正确中文含义',
    options_json = '[{"key":"A","label":"侮辱"},{"key":"B","label":"伤害"},{"key":"C","label":"命令"},{"key":"D","label":"注射"}]',
    correct_answer_json = '["A"]',
    explanation_text = '8. injurier
#题目分析：答案：A
此题为假朋友（同形异义词）。法语 injurier = “辱骂 / 侮辱”，英语形似词 injure = “伤害”。
干扰项 B “伤害”：英语 injure 的直接翻译，是强干扰项（负迁移）。
干扰项 C “命令”：作为中性填充。
干扰项 D “注射”：英语 inject，法语 injecter，词形前缀 in- + j 开头，与 injurier 部分相似，易混淆。',
    option_explanations_json = '{"B":"干扰项 B “伤害”：英语 injure 的直接翻译，是强干扰项（负迁移）。","C":"干扰项 C “命令”：作为中性填充。","D":"干扰项 D “注射”：英语 inject，法语 injecter，词形前缀 in- + j 开头，与 injurier 部分相似，易混淆。","A":"此题为假朋友（同形异义词）。法语 injurier = “辱骂 / 侮辱”，英语形似词 injure = “伤害”。"}',
    required_answer = true, weight = 1,
    transfer_category = 'FALSE_FRIEND',
    context_level = 'WORD',
    construct_code = 'LEXICAL_TRANSFER',
    target_word = 'injurier',
    display_condition_json = 'null',
    source_reference = 'questionnaire:P1A-08',
    content_hash = '506ba29d1a92604212e948702a8ab05fb1a2d65cd8e6354aaf50bc8f05219f3c'
WHERE question_bank_id = @bank_id AND question_code = 'P1A-08' AND version_no = 1 AND deleted = 0;

-- P1A-09
UPDATE assessment_question q
JOIN assessment_questionnaire_item i ON i.assessment_question_id = q.id
SET q.question_type = 'SINGLE_CHOICE',
    q.stem_text = 'se promener',
    q.prompt_text = '请选出下列法语单词对应的正确中文含义',
    q.options_json = '[{"key":"A","label":"睡觉"},{"key":"B","label":"散步"},{"key":"C","label":"购物"},{"key":"D","label":"学习"}]',
    q.correct_answer_json = '["B"]',
    q.explanation_text = '9. se promener
#题目分析：答案：B
此题为纯法语对照词。法语 se promener = “散步”，英语无直接同源词。
干扰项 A “睡觉”：法语 dormir，语义无关，但与 se promener 的“放松活动”域部分重叠。
干扰项 C “购物”：法语 faire des courses，同为外出活动。
干扰项 D “学习”：法语 étudier，语义无关，用于检测随机猜测。',
    q.score = 1,
    q.section_code = 'P1A',
    q.required_answer = true,
    q.weight = 1,
    q.transfer_category = 'COGNATE',
    q.context_level = 'WORD',
    q.construct_code = 'LEXICAL_TRANSFER',
    q.target_word = 'se promener',
    q.option_explanations_json = '{"A":"干扰项 A “睡觉”：法语 dormir，语义无关，但与 se promener 的“放松活动”域部分重叠。","C":"干扰项 C “购物”：法语 faire des courses，同为外出活动。","D":"干扰项 D “学习”：法语 étudier，语义无关，用于检测随机猜测。","B":"此题为纯法语对照词。法语 se promener = “散步”，英语无直接同源词。"}',
    q.display_condition_json = 'null'
WHERE i.questionnaire_version_id = @version_id AND i.item_code = 'P1A-09' AND i.deleted = 0 AND q.deleted = 0;
UPDATE assessment_questionnaire_item
SET required_answer = true, scored = true, weight = 1,
    transfer_category = 'COGNATE',
    context_level = 'WORD',
    construct_code = 'LEXICAL_TRANSFER',
    target_word = 'se promener',
    option_explanations_json = '{"A":"干扰项 A “睡觉”：法语 dormir，语义无关，但与 se promener 的“放松活动”域部分重叠。","C":"干扰项 C “购物”：法语 faire des courses，同为外出活动。","D":"干扰项 D “学习”：法语 étudier，语义无关，用于检测随机猜测。","B":"此题为纯法语对照词。法语 se promener = “散步”，英语无直接同源词。"}',
    display_condition_json = 'null'
WHERE questionnaire_version_id = @version_id AND item_code = 'P1A-09' AND deleted = 0;
UPDATE assessment_question_version
SET question_type = 'SINGLE_CHOICE',
    stem_text = 'se promener',
    prompt_text = '请选出下列法语单词对应的正确中文含义',
    options_json = '[{"key":"A","label":"睡觉"},{"key":"B","label":"散步"},{"key":"C","label":"购物"},{"key":"D","label":"学习"}]',
    correct_answer_json = '["B"]',
    explanation_text = '9. se promener
#题目分析：答案：B
此题为纯法语对照词。法语 se promener = “散步”，英语无直接同源词。
干扰项 A “睡觉”：法语 dormir，语义无关，但与 se promener 的“放松活动”域部分重叠。
干扰项 C “购物”：法语 faire des courses，同为外出活动。
干扰项 D “学习”：法语 étudier，语义无关，用于检测随机猜测。',
    option_explanations_json = '{"A":"干扰项 A “睡觉”：法语 dormir，语义无关，但与 se promener 的“放松活动”域部分重叠。","C":"干扰项 C “购物”：法语 faire des courses，同为外出活动。","D":"干扰项 D “学习”：法语 étudier，语义无关，用于检测随机猜测。","B":"此题为纯法语对照词。法语 se promener = “散步”，英语无直接同源词。"}',
    required_answer = true, weight = 1,
    transfer_category = 'COGNATE',
    context_level = 'WORD',
    construct_code = 'LEXICAL_TRANSFER',
    target_word = 'se promener',
    display_condition_json = 'null',
    source_reference = 'questionnaire:P1A-09',
    content_hash = '6579b003a1e37df72473a1ff4d582debc064baeab1b22362b2e2b6631dc1e486'
WHERE question_bank_id = @bank_id AND question_code = 'P1A-09' AND version_no = 1 AND deleted = 0;

-- P1A-10
UPDATE assessment_question q
JOIN assessment_questionnaire_item i ON i.assessment_question_id = q.id
SET q.question_type = 'SINGLE_CHOICE',
    q.stem_text = 'respecter',
    q.prompt_text = '请选出下列法语单词对应的正确中文含义',
    q.options_json = '[{"key":"A","label":"尊敬"},{"key":"B","label":"怀疑"},{"key":"C","label":"回应"},{"key":"D","label":"剥削"}]',
    q.correct_answer_json = '["A"]',
    q.explanation_text = '10. respecter
#题目分析：答案：A
此题为英法同源词。法语 respecter = “尊敬 / 遵守”，同源英语 respect = “尊敬”。
干扰项 B “怀疑”：英语 suspect，法语 soupçonner，词形部分相似（respect / suspect），共享 -spect 词根（看）。
干扰项 C “回应”：英语 respond，法语 répondre，无直接形似，但 re- 前缀可引发干扰。
干扰项 D “分别”：英语 respective （各自的），与 respecter 共享词根 respect-，但语义完全不同，容易因形近而误选。
（8、9、10为一组对应）',
    q.score = 1,
    q.section_code = 'P1A',
    q.required_answer = true,
    q.weight = 1,
    q.transfer_category = 'COGNATE',
    q.context_level = 'WORD',
    q.construct_code = 'LEXICAL_TRANSFER',
    q.target_word = 'respecter',
    q.option_explanations_json = '{"B":"干扰项 B “怀疑”：英语 suspect，法语 soupçonner，词形部分相似（respect / suspect），共享 -spect 词根（看）。","C":"干扰项 C “回应”：英语 respond，法语 répondre，无直接形似，但 re- 前缀可引发干扰。","D":"干扰项 D “分别”：英语 respective （各自的），与 respecter 共享词根 respect-，但语义完全不同，容易因形近而误选。","A":"此题为英法同源词。法语 respecter = “尊敬 / 遵守”，同源英语 respect = “尊敬”。\\n（8、9、10为一组对应）"}',
    q.display_condition_json = 'null'
WHERE i.questionnaire_version_id = @version_id AND i.item_code = 'P1A-10' AND i.deleted = 0 AND q.deleted = 0;
UPDATE assessment_questionnaire_item
SET required_answer = true, scored = true, weight = 1,
    transfer_category = 'COGNATE',
    context_level = 'WORD',
    construct_code = 'LEXICAL_TRANSFER',
    target_word = 'respecter',
    option_explanations_json = '{"B":"干扰项 B “怀疑”：英语 suspect，法语 soupçonner，词形部分相似（respect / suspect），共享 -spect 词根（看）。","C":"干扰项 C “回应”：英语 respond，法语 répondre，无直接形似，但 re- 前缀可引发干扰。","D":"干扰项 D “分别”：英语 respective （各自的），与 respecter 共享词根 respect-，但语义完全不同，容易因形近而误选。","A":"此题为英法同源词。法语 respecter = “尊敬 / 遵守”，同源英语 respect = “尊敬”。\\n（8、9、10为一组对应）"}',
    display_condition_json = 'null'
WHERE questionnaire_version_id = @version_id AND item_code = 'P1A-10' AND deleted = 0;
UPDATE assessment_question_version
SET question_type = 'SINGLE_CHOICE',
    stem_text = 'respecter',
    prompt_text = '请选出下列法语单词对应的正确中文含义',
    options_json = '[{"key":"A","label":"尊敬"},{"key":"B","label":"怀疑"},{"key":"C","label":"回应"},{"key":"D","label":"剥削"}]',
    correct_answer_json = '["A"]',
    explanation_text = '10. respecter
#题目分析：答案：A
此题为英法同源词。法语 respecter = “尊敬 / 遵守”，同源英语 respect = “尊敬”。
干扰项 B “怀疑”：英语 suspect，法语 soupçonner，词形部分相似（respect / suspect），共享 -spect 词根（看）。
干扰项 C “回应”：英语 respond，法语 répondre，无直接形似，但 re- 前缀可引发干扰。
干扰项 D “分别”：英语 respective （各自的），与 respecter 共享词根 respect-，但语义完全不同，容易因形近而误选。
（8、9、10为一组对应）',
    option_explanations_json = '{"B":"干扰项 B “怀疑”：英语 suspect，法语 soupçonner，词形部分相似（respect / suspect），共享 -spect 词根（看）。","C":"干扰项 C “回应”：英语 respond，法语 répondre，无直接形似，但 re- 前缀可引发干扰。","D":"干扰项 D “分别”：英语 respective （各自的），与 respecter 共享词根 respect-，但语义完全不同，容易因形近而误选。","A":"此题为英法同源词。法语 respecter = “尊敬 / 遵守”，同源英语 respect = “尊敬”。\\n（8、9、10为一组对应）"}',
    required_answer = true, weight = 1,
    transfer_category = 'COGNATE',
    context_level = 'WORD',
    construct_code = 'LEXICAL_TRANSFER',
    target_word = 'respecter',
    display_condition_json = 'null',
    source_reference = 'questionnaire:P1A-10',
    content_hash = 'a1935a99095bed1de8b475655d9bb58f5744db8a1746bb4ff193c08ec916724d'
WHERE question_bank_id = @bank_id AND question_code = 'P1A-10' AND version_no = 1 AND deleted = 0;

-- P1B-01
UPDATE assessment_question q
JOIN assessment_questionnaire_item i ON i.assessment_question_id = q.id
SET q.question_type = 'SINGLE_CHOICE',
    q.stem_text = 'une **entreprise** publique',
    q.prompt_text = '请选出与下列短语中划线单词意思相同的选项',
    q.options_json = '[{"key":"A","label":"réunion"},{"key":"B","label":"compagnie"},{"key":"C","label":"exploitation"},{"key":"D","label":"organisation"}]',
    q.correct_answer_json = '["B"]',
    q.explanation_text = '1. une entreprise publique
#题目分析：答案：B
此题为英法同源词。法语 entreprise = “企业”，同源英语 enterprise = “企业/事业”。
干扰项 A “会议”（réunion）：语义无关，但词形与英语 reunion 相似，可能吸引注意力。
干扰项 C “开发”（exploitation）：语义部分重叠（企业运营），但非直接同义。
干扰项 D “组织”（organisation）：语义域相近（机构类），但非精确同义。',
    q.score = 1,
    q.section_code = 'P1B',
    q.required_answer = true,
    q.weight = 1,
    q.transfer_category = 'COGNATE',
    q.context_level = 'PHRASE',
    q.construct_code = 'LEXICAL_TRANSFER',
    q.target_word = 'entreprise',
    q.option_explanations_json = '{"A":"干扰项 A “会议”（réunion）：语义无关，但词形与英语 reunion 相似，可能吸引注意力。","C":"干扰项 C “开发”（exploitation）：语义部分重叠（企业运营），但非直接同义。","D":"干扰项 D “组织”（organisation）：语义域相近（机构类），但非精确同义。","B":"此题为英法同源词。法语 entreprise = “企业”，同源英语 enterprise = “企业/事业”。"}',
    q.display_condition_json = 'null'
WHERE i.questionnaire_version_id = @version_id AND i.item_code = 'P1B-01' AND i.deleted = 0 AND q.deleted = 0;
UPDATE assessment_questionnaire_item
SET required_answer = true, scored = true, weight = 1,
    transfer_category = 'COGNATE',
    context_level = 'PHRASE',
    construct_code = 'LEXICAL_TRANSFER',
    target_word = 'entreprise',
    option_explanations_json = '{"A":"干扰项 A “会议”（réunion）：语义无关，但词形与英语 reunion 相似，可能吸引注意力。","C":"干扰项 C “开发”（exploitation）：语义部分重叠（企业运营），但非直接同义。","D":"干扰项 D “组织”（organisation）：语义域相近（机构类），但非精确同义。","B":"此题为英法同源词。法语 entreprise = “企业”，同源英语 enterprise = “企业/事业”。"}',
    display_condition_json = 'null'
WHERE questionnaire_version_id = @version_id AND item_code = 'P1B-01' AND deleted = 0;
UPDATE assessment_question_version
SET question_type = 'SINGLE_CHOICE',
    stem_text = 'une **entreprise** publique',
    prompt_text = '请选出与下列短语中划线单词意思相同的选项',
    options_json = '[{"key":"A","label":"réunion"},{"key":"B","label":"compagnie"},{"key":"C","label":"exploitation"},{"key":"D","label":"organisation"}]',
    correct_answer_json = '["B"]',
    explanation_text = '1. une entreprise publique
#题目分析：答案：B
此题为英法同源词。法语 entreprise = “企业”，同源英语 enterprise = “企业/事业”。
干扰项 A “会议”（réunion）：语义无关，但词形与英语 reunion 相似，可能吸引注意力。
干扰项 C “开发”（exploitation）：语义部分重叠（企业运营），但非直接同义。
干扰项 D “组织”（organisation）：语义域相近（机构类），但非精确同义。',
    option_explanations_json = '{"A":"干扰项 A “会议”（réunion）：语义无关，但词形与英语 reunion 相似，可能吸引注意力。","C":"干扰项 C “开发”（exploitation）：语义部分重叠（企业运营），但非直接同义。","D":"干扰项 D “组织”（organisation）：语义域相近（机构类），但非精确同义。","B":"此题为英法同源词。法语 entreprise = “企业”，同源英语 enterprise = “企业/事业”。"}',
    required_answer = true, weight = 1,
    transfer_category = 'COGNATE',
    context_level = 'PHRASE',
    construct_code = 'LEXICAL_TRANSFER',
    target_word = 'entreprise',
    display_condition_json = 'null',
    source_reference = 'questionnaire:P1B-01',
    content_hash = '991807ff6c3f84b39f27339826444ae3fb3408662db1f8871d77b4c0c7e11111'
WHERE question_bank_id = @bank_id AND question_code = 'P1B-01' AND version_no = 1 AND deleted = 0;

-- P1B-02
UPDATE assessment_question q
JOIN assessment_questionnaire_item i ON i.assessment_question_id = q.id
SET q.question_type = 'SINGLE_CHOICE',
    q.stem_text = 'faire des **courses**',
    q.prompt_text = '请选出与下列短语中划线单词意思相同的选项',
    q.options_json = '[{"key":"A","label":"achats"},{"key":"B","label":"leçons"},{"key":"C","label":"examens"},{"key":"D","label":"exercices"}]',
    q.correct_answer_json = '["A"]',
    q.explanation_text = '2. faire des courses
#题目分析：答案：A
此题为假朋友（同形异义词）。法语 courses（复数）= “购物”，英语形似词 course = “课程 / 路线”。
干扰项 B “课程”（leçons）：英语 course 的主要翻译，是负迁移的主要来源。
干扰项 C “考试”（examens）：学习相关语义域，但与 course 无直接形似。
干扰项 D “练习”（exercices）：同样属于学习场景，用于检测语义泛化。',
    q.score = 1,
    q.section_code = 'P1B',
    q.required_answer = true,
    q.weight = 1,
    q.transfer_category = 'FALSE_FRIEND',
    q.context_level = 'PHRASE',
    q.construct_code = 'LEXICAL_TRANSFER',
    q.target_word = 'courses',
    q.option_explanations_json = '{"B":"干扰项 B “课程”（leçons）：英语 course 的主要翻译，是负迁移的主要来源。","C":"干扰项 C “考试”（examens）：学习相关语义域，但与 course 无直接形似。","D":"干扰项 D “练习”（exercices）：同样属于学习场景，用于检测语义泛化。","A":"此题为假朋友（同形异义词）。法语 courses（复数）= “购物”，英语形似词 course = “课程 / 路线”。"}',
    q.display_condition_json = 'null'
WHERE i.questionnaire_version_id = @version_id AND i.item_code = 'P1B-02' AND i.deleted = 0 AND q.deleted = 0;
UPDATE assessment_questionnaire_item
SET required_answer = true, scored = true, weight = 1,
    transfer_category = 'FALSE_FRIEND',
    context_level = 'PHRASE',
    construct_code = 'LEXICAL_TRANSFER',
    target_word = 'courses',
    option_explanations_json = '{"B":"干扰项 B “课程”（leçons）：英语 course 的主要翻译，是负迁移的主要来源。","C":"干扰项 C “考试”（examens）：学习相关语义域，但与 course 无直接形似。","D":"干扰项 D “练习”（exercices）：同样属于学习场景，用于检测语义泛化。","A":"此题为假朋友（同形异义词）。法语 courses（复数）= “购物”，英语形似词 course = “课程 / 路线”。"}',
    display_condition_json = 'null'
WHERE questionnaire_version_id = @version_id AND item_code = 'P1B-02' AND deleted = 0;
UPDATE assessment_question_version
SET question_type = 'SINGLE_CHOICE',
    stem_text = 'faire des **courses**',
    prompt_text = '请选出与下列短语中划线单词意思相同的选项',
    options_json = '[{"key":"A","label":"achats"},{"key":"B","label":"leçons"},{"key":"C","label":"examens"},{"key":"D","label":"exercices"}]',
    correct_answer_json = '["A"]',
    explanation_text = '2. faire des courses
#题目分析：答案：A
此题为假朋友（同形异义词）。法语 courses（复数）= “购物”，英语形似词 course = “课程 / 路线”。
干扰项 B “课程”（leçons）：英语 course 的主要翻译，是负迁移的主要来源。
干扰项 C “考试”（examens）：学习相关语义域，但与 course 无直接形似。
干扰项 D “练习”（exercices）：同样属于学习场景，用于检测语义泛化。',
    option_explanations_json = '{"B":"干扰项 B “课程”（leçons）：英语 course 的主要翻译，是负迁移的主要来源。","C":"干扰项 C “考试”（examens）：学习相关语义域，但与 course 无直接形似。","D":"干扰项 D “练习”（exercices）：同样属于学习场景，用于检测语义泛化。","A":"此题为假朋友（同形异义词）。法语 courses（复数）= “购物”，英语形似词 course = “课程 / 路线”。"}',
    required_answer = true, weight = 1,
    transfer_category = 'FALSE_FRIEND',
    context_level = 'PHRASE',
    construct_code = 'LEXICAL_TRANSFER',
    target_word = 'courses',
    display_condition_json = 'null',
    source_reference = 'questionnaire:P1B-02',
    content_hash = 'c562ef395e351817ccdf73612525057bc281e97ec82162170d76d8097f068997'
WHERE question_bank_id = @bank_id AND question_code = 'P1B-02' AND version_no = 1 AND deleted = 0;

-- P1B-03
UPDATE assessment_question q
JOIN assessment_questionnaire_item i ON i.assessment_question_id = q.id
SET q.question_type = 'SINGLE_CHOICE',
    q.stem_text = 'une **boisson** sucrée',
    q.prompt_text = '请选出与下列短语中划线单词意思相同的选项',
    q.options_json = '[{"key":"A","label":"liquide à boire"},{"key":"B","label":"nourriture"},{"key":"C","label":"médicament"},{"key":"D","label":"alcool"}]',
    q.correct_answer_json = '["A"]',
    q.explanation_text = '3. une boisson sucrée
#题目分析：答案：A
此题为纯法语对照词。法语 boisson = “饮品”，英语无直接同源词。
干扰项 B “食物”（nourriture）：与“饮品”同属餐饮语义域，日常表达中常并列出现，易混淆。
干扰项 C “药品”（médicament）：部分饮品（如糖浆类）与药品边界模糊，且“服用”场景相似。
干扰项 D “酒精”（alcool）：酒精属于饮品的一种，学生可能因“常见的饮品类型”而过度细化，误将下位词当作同义解释。
（1、2、3题为一组对应）',
    q.score = 1,
    q.section_code = 'P1B',
    q.required_answer = true,
    q.weight = 1,
    q.transfer_category = 'COGNATE',
    q.context_level = 'PHRASE',
    q.construct_code = 'LEXICAL_TRANSFER',
    q.target_word = 'boisson',
    q.option_explanations_json = '{"B":"干扰项 B “食物”（nourriture）：与“饮品”同属餐饮语义域，日常表达中常并列出现，易混淆。","C":"干扰项 C “药品”（médicament）：部分饮品（如糖浆类）与药品边界模糊，且“服用”场景相似。","D":"干扰项 D “酒精”（alcool）：酒精属于饮品的一种，学生可能因“常见的饮品类型”而过度细化，误将下位词当作同义解释。","A":"此题为纯法语对照词。法语 boisson = “饮品”，英语无直接同源词。\\n（1、2、3题为一组对应）"}',
    q.display_condition_json = 'null'
WHERE i.questionnaire_version_id = @version_id AND i.item_code = 'P1B-03' AND i.deleted = 0 AND q.deleted = 0;
UPDATE assessment_questionnaire_item
SET required_answer = true, scored = true, weight = 1,
    transfer_category = 'COGNATE',
    context_level = 'PHRASE',
    construct_code = 'LEXICAL_TRANSFER',
    target_word = 'boisson',
    option_explanations_json = '{"B":"干扰项 B “食物”（nourriture）：与“饮品”同属餐饮语义域，日常表达中常并列出现，易混淆。","C":"干扰项 C “药品”（médicament）：部分饮品（如糖浆类）与药品边界模糊，且“服用”场景相似。","D":"干扰项 D “酒精”（alcool）：酒精属于饮品的一种，学生可能因“常见的饮品类型”而过度细化，误将下位词当作同义解释。","A":"此题为纯法语对照词。法语 boisson = “饮品”，英语无直接同源词。\\n（1、2、3题为一组对应）"}',
    display_condition_json = 'null'
WHERE questionnaire_version_id = @version_id AND item_code = 'P1B-03' AND deleted = 0;
UPDATE assessment_question_version
SET question_type = 'SINGLE_CHOICE',
    stem_text = 'une **boisson** sucrée',
    prompt_text = '请选出与下列短语中划线单词意思相同的选项',
    options_json = '[{"key":"A","label":"liquide à boire"},{"key":"B","label":"nourriture"},{"key":"C","label":"médicament"},{"key":"D","label":"alcool"}]',
    correct_answer_json = '["A"]',
    explanation_text = '3. une boisson sucrée
#题目分析：答案：A
此题为纯法语对照词。法语 boisson = “饮品”，英语无直接同源词。
干扰项 B “食物”（nourriture）：与“饮品”同属餐饮语义域，日常表达中常并列出现，易混淆。
干扰项 C “药品”（médicament）：部分饮品（如糖浆类）与药品边界模糊，且“服用”场景相似。
干扰项 D “酒精”（alcool）：酒精属于饮品的一种，学生可能因“常见的饮品类型”而过度细化，误将下位词当作同义解释。
（1、2、3题为一组对应）',
    option_explanations_json = '{"B":"干扰项 B “食物”（nourriture）：与“饮品”同属餐饮语义域，日常表达中常并列出现，易混淆。","C":"干扰项 C “药品”（médicament）：部分饮品（如糖浆类）与药品边界模糊，且“服用”场景相似。","D":"干扰项 D “酒精”（alcool）：酒精属于饮品的一种，学生可能因“常见的饮品类型”而过度细化，误将下位词当作同义解释。","A":"此题为纯法语对照词。法语 boisson = “饮品”，英语无直接同源词。\\n（1、2、3题为一组对应）"}',
    required_answer = true, weight = 1,
    transfer_category = 'COGNATE',
    context_level = 'PHRASE',
    construct_code = 'LEXICAL_TRANSFER',
    target_word = 'boisson',
    display_condition_json = 'null',
    source_reference = 'questionnaire:P1B-03',
    content_hash = 'ed051ea1f933797983d73c4febcc555a70bab83f44765f5aed2ce16049f50310'
WHERE question_bank_id = @bank_id AND question_code = 'P1B-03' AND version_no = 1 AND deleted = 0;

-- P1B-04
UPDATE assessment_question q
JOIN assessment_questionnaire_item i ON i.assessment_question_id = q.id
SET q.question_type = 'SINGLE_CHOICE',
    q.stem_text = '**blesser** mon amie',
    q.prompt_text = '请选出与下列短语中划线单词意思相同的选项',
    q.options_json = '[{"key":"A","label":"aider"},{"key":"B","label":"souhaité le bonheur à"},{"key":"C","label":"fait du mal à"},{"key":"D","label":"sauver la vie à"}]',
    q.correct_answer_json = '["C"]',
    q.explanation_text = '4. blesser mon amie
#题目分析：答案：C
此题为假朋友（同形异义词）。法语 blesser = “使受伤”，英语形似词 bless = “祝福”。
干扰项 A “帮助”（aider）：语义相反。
干扰项 B “祝她幸福”（souhaité le bonheur à）：英语 bless 的典型翻译，是强干扰项。
干扰项 D “救她的命”（sauver la vie à）：语义过强，但属于“善意行为”语义域。',
    q.score = 1,
    q.section_code = 'P1B',
    q.required_answer = true,
    q.weight = 1,
    q.transfer_category = 'FALSE_FRIEND',
    q.context_level = 'PHRASE',
    q.construct_code = 'LEXICAL_TRANSFER',
    q.target_word = 'blesser',
    q.option_explanations_json = '{"A":"干扰项 A “帮助”（aider）：语义相反。","B":"干扰项 B “祝她幸福”（souhaité le bonheur à）：英语 bless 的典型翻译，是强干扰项。","D":"干扰项 D “救她的命”（sauver la vie à）：语义过强，但属于“善意行为”语义域。","C":"此题为假朋友（同形异义词）。法语 blesser = “使受伤”，英语形似词 bless = “祝福”。"}',
    q.display_condition_json = 'null'
WHERE i.questionnaire_version_id = @version_id AND i.item_code = 'P1B-04' AND i.deleted = 0 AND q.deleted = 0;
UPDATE assessment_questionnaire_item
SET required_answer = true, scored = true, weight = 1,
    transfer_category = 'FALSE_FRIEND',
    context_level = 'PHRASE',
    construct_code = 'LEXICAL_TRANSFER',
    target_word = 'blesser',
    option_explanations_json = '{"A":"干扰项 A “帮助”（aider）：语义相反。","B":"干扰项 B “祝她幸福”（souhaité le bonheur à）：英语 bless 的典型翻译，是强干扰项。","D":"干扰项 D “救她的命”（sauver la vie à）：语义过强，但属于“善意行为”语义域。","C":"此题为假朋友（同形异义词）。法语 blesser = “使受伤”，英语形似词 bless = “祝福”。"}',
    display_condition_json = 'null'
WHERE questionnaire_version_id = @version_id AND item_code = 'P1B-04' AND deleted = 0;
UPDATE assessment_question_version
SET question_type = 'SINGLE_CHOICE',
    stem_text = '**blesser** mon amie',
    prompt_text = '请选出与下列短语中划线单词意思相同的选项',
    options_json = '[{"key":"A","label":"aider"},{"key":"B","label":"souhaité le bonheur à"},{"key":"C","label":"fait du mal à"},{"key":"D","label":"sauver la vie à"}]',
    correct_answer_json = '["C"]',
    explanation_text = '4. blesser mon amie
#题目分析：答案：C
此题为假朋友（同形异义词）。法语 blesser = “使受伤”，英语形似词 bless = “祝福”。
干扰项 A “帮助”（aider）：语义相反。
干扰项 B “祝她幸福”（souhaité le bonheur à）：英语 bless 的典型翻译，是强干扰项。
干扰项 D “救她的命”（sauver la vie à）：语义过强，但属于“善意行为”语义域。',
    option_explanations_json = '{"A":"干扰项 A “帮助”（aider）：语义相反。","B":"干扰项 B “祝她幸福”（souhaité le bonheur à）：英语 bless 的典型翻译，是强干扰项。","D":"干扰项 D “救她的命”（sauver la vie à）：语义过强，但属于“善意行为”语义域。","C":"此题为假朋友（同形异义词）。法语 blesser = “使受伤”，英语形似词 bless = “祝福”。"}',
    required_answer = true, weight = 1,
    transfer_category = 'FALSE_FRIEND',
    context_level = 'PHRASE',
    construct_code = 'LEXICAL_TRANSFER',
    target_word = 'blesser',
    display_condition_json = 'null',
    source_reference = 'questionnaire:P1B-04',
    content_hash = 'c63b3cb40d2deded11831a96f813ca898a53f486527a8e37622dd0e3b694f4d5'
WHERE question_bank_id = @bank_id AND question_code = 'P1B-04' AND version_no = 1 AND deleted = 0;

-- P1B-05
UPDATE assessment_question q
JOIN assessment_questionnaire_item i ON i.assessment_question_id = q.id
SET q.question_type = 'SINGLE_CHOICE',
    q.stem_text = '**accomplir** une tâche',
    q.prompt_text = '请选出与下列短语中划线单词意思相同的选项',
    q.options_json = '[{"key":"A","label":"faire"},{"key":"B","label":"réaliser"},{"key":"C","label":"préparer"},{"key":"D","label":"accepter"}]',
    q.correct_answer_json = '["B"]',
    q.explanation_text = '5. accomplir une tâche
#题目分析：答案：B
此题为英法同源词。法语 accomplir = “完成 / 实现”，同源英语 accomplish = “完成”。
干扰项 A “做”（faire）：语义部分重叠，但“做”不等于“完成”，用于检测语义精确度。
干扰项 C “准备”（préparer）：任务完成前的行为，语义相近但不同。
干扰项 D “接受”（accepter）：语义无关，用于检测随机选择。
（4、5为一组对应）
请选出与下列短语中划线单词意思相反的选项',
    q.score = 1,
    q.section_code = 'P1B',
    q.required_answer = true,
    q.weight = 1,
    q.transfer_category = 'COGNATE',
    q.context_level = 'PHRASE',
    q.construct_code = 'LEXICAL_TRANSFER',
    q.target_word = 'accomplir',
    q.option_explanations_json = '{"A":"干扰项 A “做”（faire）：语义部分重叠，但“做”不等于“完成”，用于检测语义精确度。","C":"干扰项 C “准备”（préparer）：任务完成前的行为，语义相近但不同。","D":"干扰项 D “接受”（accepter）：语义无关，用于检测随机选择。","B":"此题为英法同源词。法语 accomplir = “完成 / 实现”，同源英语 accomplish = “完成”。\\n（4、5为一组对应）\\n请选出与下列短语中划线单词意思相反的选项"}',
    q.display_condition_json = 'null'
WHERE i.questionnaire_version_id = @version_id AND i.item_code = 'P1B-05' AND i.deleted = 0 AND q.deleted = 0;
UPDATE assessment_questionnaire_item
SET required_answer = true, scored = true, weight = 1,
    transfer_category = 'COGNATE',
    context_level = 'PHRASE',
    construct_code = 'LEXICAL_TRANSFER',
    target_word = 'accomplir',
    option_explanations_json = '{"A":"干扰项 A “做”（faire）：语义部分重叠，但“做”不等于“完成”，用于检测语义精确度。","C":"干扰项 C “准备”（préparer）：任务完成前的行为，语义相近但不同。","D":"干扰项 D “接受”（accepter）：语义无关，用于检测随机选择。","B":"此题为英法同源词。法语 accomplir = “完成 / 实现”，同源英语 accomplish = “完成”。\\n（4、5为一组对应）\\n请选出与下列短语中划线单词意思相反的选项"}',
    display_condition_json = 'null'
WHERE questionnaire_version_id = @version_id AND item_code = 'P1B-05' AND deleted = 0;
UPDATE assessment_question_version
SET question_type = 'SINGLE_CHOICE',
    stem_text = '**accomplir** une tâche',
    prompt_text = '请选出与下列短语中划线单词意思相同的选项',
    options_json = '[{"key":"A","label":"faire"},{"key":"B","label":"réaliser"},{"key":"C","label":"préparer"},{"key":"D","label":"accepter"}]',
    correct_answer_json = '["B"]',
    explanation_text = '5. accomplir une tâche
#题目分析：答案：B
此题为英法同源词。法语 accomplir = “完成 / 实现”，同源英语 accomplish = “完成”。
干扰项 A “做”（faire）：语义部分重叠，但“做”不等于“完成”，用于检测语义精确度。
干扰项 C “准备”（préparer）：任务完成前的行为，语义相近但不同。
干扰项 D “接受”（accepter）：语义无关，用于检测随机选择。
（4、5为一组对应）
请选出与下列短语中划线单词意思相反的选项',
    option_explanations_json = '{"A":"干扰项 A “做”（faire）：语义部分重叠，但“做”不等于“完成”，用于检测语义精确度。","C":"干扰项 C “准备”（préparer）：任务完成前的行为，语义相近但不同。","D":"干扰项 D “接受”（accepter）：语义无关，用于检测随机选择。","B":"此题为英法同源词。法语 accomplir = “完成 / 实现”，同源英语 accomplish = “完成”。\\n（4、5为一组对应）\\n请选出与下列短语中划线单词意思相反的选项"}',
    required_answer = true, weight = 1,
    transfer_category = 'COGNATE',
    context_level = 'PHRASE',
    construct_code = 'LEXICAL_TRANSFER',
    target_word = 'accomplir',
    display_condition_json = 'null',
    source_reference = 'questionnaire:P1B-05',
    content_hash = '8922faa2c59d64d37b5a7e5647c19adeba7d6f5fe8329cf077f525601067360f'
WHERE question_bank_id = @bank_id AND question_code = 'P1B-05' AND version_no = 1 AND deleted = 0;

-- P1B-06
UPDATE assessment_question q
JOIN assessment_questionnaire_item i ON i.assessment_question_id = q.id
SET q.question_type = 'SINGLE_CHOICE',
    q.stem_text = 'un problème **important**',
    q.prompt_text = '请选出与下列短语中划线单词意思相反的选项',
    q.options_json = '[{"key":"A","label":"petit"},{"key":"B","label":"insignifiant"},{"key":"C","label":"grand"},{"key":"D","label":"essentiel"}]',
    q.correct_answer_json = '["B"]',
    q.explanation_text = '6. un problème important
#题目分析：答案：B
此题为英法同源词。法语 important = “重要的”，同源英语 important = “重要的”。
干扰项 A “小的”（petit）：可与 important 形成对比，但非标准反义词。
干扰项 C “大的”（grand）：语义接近 important，非反义。
干扰项 D “基本的”（essentiel）：语义近似 important，非反义。',
    q.score = 1,
    q.section_code = 'P1B',
    q.required_answer = true,
    q.weight = 1,
    q.transfer_category = 'COGNATE',
    q.context_level = 'PHRASE',
    q.construct_code = 'LEXICAL_TRANSFER',
    q.target_word = 'important',
    q.option_explanations_json = '{"A":"干扰项 A “小的”（petit）：可与 important 形成对比，但非标准反义词。","C":"干扰项 C “大的”（grand）：语义接近 important，非反义。","D":"干扰项 D “基本的”（essentiel）：语义近似 important，非反义。","B":"此题为英法同源词。法语 important = “重要的”，同源英语 important = “重要的”。"}',
    q.display_condition_json = 'null'
WHERE i.questionnaire_version_id = @version_id AND i.item_code = 'P1B-06' AND i.deleted = 0 AND q.deleted = 0;
UPDATE assessment_questionnaire_item
SET required_answer = true, scored = true, weight = 1,
    transfer_category = 'COGNATE',
    context_level = 'PHRASE',
    construct_code = 'LEXICAL_TRANSFER',
    target_word = 'important',
    option_explanations_json = '{"A":"干扰项 A “小的”（petit）：可与 important 形成对比，但非标准反义词。","C":"干扰项 C “大的”（grand）：语义接近 important，非反义。","D":"干扰项 D “基本的”（essentiel）：语义近似 important，非反义。","B":"此题为英法同源词。法语 important = “重要的”，同源英语 important = “重要的”。"}',
    display_condition_json = 'null'
WHERE questionnaire_version_id = @version_id AND item_code = 'P1B-06' AND deleted = 0;
UPDATE assessment_question_version
SET question_type = 'SINGLE_CHOICE',
    stem_text = 'un problème **important**',
    prompt_text = '请选出与下列短语中划线单词意思相反的选项',
    options_json = '[{"key":"A","label":"petit"},{"key":"B","label":"insignifiant"},{"key":"C","label":"grand"},{"key":"D","label":"essentiel"}]',
    correct_answer_json = '["B"]',
    explanation_text = '6. un problème important
#题目分析：答案：B
此题为英法同源词。法语 important = “重要的”，同源英语 important = “重要的”。
干扰项 A “小的”（petit）：可与 important 形成对比，但非标准反义词。
干扰项 C “大的”（grand）：语义接近 important，非反义。
干扰项 D “基本的”（essentiel）：语义近似 important，非反义。',
    option_explanations_json = '{"A":"干扰项 A “小的”（petit）：可与 important 形成对比，但非标准反义词。","C":"干扰项 C “大的”（grand）：语义接近 important，非反义。","D":"干扰项 D “基本的”（essentiel）：语义近似 important，非反义。","B":"此题为英法同源词。法语 important = “重要的”，同源英语 important = “重要的”。"}',
    required_answer = true, weight = 1,
    transfer_category = 'COGNATE',
    context_level = 'PHRASE',
    construct_code = 'LEXICAL_TRANSFER',
    target_word = 'important',
    display_condition_json = 'null',
    source_reference = 'questionnaire:P1B-06',
    content_hash = '33337daebec091e72d37d5ed2301bb47b50f4cfd36728404b13b7e3d91d1aaec'
WHERE question_bank_id = @bank_id AND question_code = 'P1B-06' AND version_no = 1 AND deleted = 0;

-- P1B-07
UPDATE assessment_question q
JOIN assessment_questionnaire_item i ON i.assessment_question_id = q.id
SET q.question_type = 'SINGLE_CHOICE',
    q.stem_text = 'un film **ennuyeux**',
    q.prompt_text = '请选出与下列短语中划线单词意思相反的选项',
    q.options_json = '[{"key":"A","label":"amusant"},{"key":"B","label":"court"},{"key":"C","label":"vieux"},{"key":"D","label":"simple"}]',
    q.correct_answer_json = '["A"]',
    q.explanation_text = '7. un film ennuyeux
#题目分析：答案：A
此题为纯法语对照词。法语 ennuyeux = “无聊的”，英语无直接同源词。
干扰项 B “短的”（court）：无关语义。
干扰项 C “老的”（vieux）：无关语义。
干扰项 D “简单的”（simple）：无关语义。',
    q.score = 1,
    q.section_code = 'P1B',
    q.required_answer = true,
    q.weight = 1,
    q.transfer_category = 'COGNATE',
    q.context_level = 'PHRASE',
    q.construct_code = 'LEXICAL_TRANSFER',
    q.target_word = 'ennuyeux',
    q.option_explanations_json = '{"B":"干扰项 B “短的”（court）：无关语义。","C":"干扰项 C “老的”（vieux）：无关语义。","D":"干扰项 D “简单的”（simple）：无关语义。","A":"此题为纯法语对照词。法语 ennuyeux = “无聊的”，英语无直接同源词。"}',
    q.display_condition_json = 'null'
WHERE i.questionnaire_version_id = @version_id AND i.item_code = 'P1B-07' AND i.deleted = 0 AND q.deleted = 0;
UPDATE assessment_questionnaire_item
SET required_answer = true, scored = true, weight = 1,
    transfer_category = 'COGNATE',
    context_level = 'PHRASE',
    construct_code = 'LEXICAL_TRANSFER',
    target_word = 'ennuyeux',
    option_explanations_json = '{"B":"干扰项 B “短的”（court）：无关语义。","C":"干扰项 C “老的”（vieux）：无关语义。","D":"干扰项 D “简单的”（simple）：无关语义。","A":"此题为纯法语对照词。法语 ennuyeux = “无聊的”，英语无直接同源词。"}',
    display_condition_json = 'null'
WHERE questionnaire_version_id = @version_id AND item_code = 'P1B-07' AND deleted = 0;
UPDATE assessment_question_version
SET question_type = 'SINGLE_CHOICE',
    stem_text = 'un film **ennuyeux**',
    prompt_text = '请选出与下列短语中划线单词意思相反的选项',
    options_json = '[{"key":"A","label":"amusant"},{"key":"B","label":"court"},{"key":"C","label":"vieux"},{"key":"D","label":"simple"}]',
    correct_answer_json = '["A"]',
    explanation_text = '7. un film ennuyeux
#题目分析：答案：A
此题为纯法语对照词。法语 ennuyeux = “无聊的”，英语无直接同源词。
干扰项 B “短的”（court）：无关语义。
干扰项 C “老的”（vieux）：无关语义。
干扰项 D “简单的”（simple）：无关语义。',
    option_explanations_json = '{"B":"干扰项 B “短的”（court）：无关语义。","C":"干扰项 C “老的”（vieux）：无关语义。","D":"干扰项 D “简单的”（simple）：无关语义。","A":"此题为纯法语对照词。法语 ennuyeux = “无聊的”，英语无直接同源词。"}',
    required_answer = true, weight = 1,
    transfer_category = 'COGNATE',
    context_level = 'PHRASE',
    construct_code = 'LEXICAL_TRANSFER',
    target_word = 'ennuyeux',
    display_condition_json = 'null',
    source_reference = 'questionnaire:P1B-07',
    content_hash = '167dc4795de115a4a69278ed6c4bb625e185a2376184d119adf0d2bd61031863'
WHERE question_bank_id = @bank_id AND question_code = 'P1B-07' AND version_no = 1 AND deleted = 0;

-- P1B-08
UPDATE assessment_question q
JOIN assessment_questionnaire_item i ON i.assessment_question_id = q.id
SET q.question_type = 'SINGLE_CHOICE',
    q.stem_text = 'une personne **sensible**',
    q.prompt_text = '请选出与下列短语中划线单词意思相反的选项',
    q.options_json = '[{"key":"A","label":"raisonnable"},{"key":"B","label":"irrationnelle"},{"key":"C","label":"logique"},{"key":"D","label":"indifférente"}]',
    q.correct_answer_json = '["D"]',
    q.explanation_text = '8. une personne sensible
#题目分析：答案：D
此题为假朋友（同形异义词）。法语 sensible = “敏感的”，英语形似词 sensible = “明智的”。
干扰项 A “理性的”（raisonnable）：英语 sensible 的近义词，是强干扰项。
干扰项 B “不理性的”（irrationnelle）：英语 sensible 的反义词，形成认知对比。
干扰项 C “合乎逻辑的”（logique）：英语 sensible 的语义域成员。
（11、12、13题为一组对应）',
    q.score = 1,
    q.section_code = 'P1B',
    q.required_answer = true,
    q.weight = 1,
    q.transfer_category = 'FALSE_FRIEND',
    q.context_level = 'PHRASE',
    q.construct_code = 'LEXICAL_TRANSFER',
    q.target_word = 'sensible',
    q.option_explanations_json = '{"A":"干扰项 A “理性的”（raisonnable）：英语 sensible 的近义词，是强干扰项。","B":"干扰项 B “不理性的”（irrationnelle）：英语 sensible 的反义词，形成认知对比。","C":"干扰项 C “合乎逻辑的”（logique）：英语 sensible 的语义域成员。","D":"此题为假朋友（同形异义词）。法语 sensible = “敏感的”，英语形似词 sensible = “明智的”。\\n（11、12、13题为一组对应）"}',
    q.display_condition_json = 'null'
WHERE i.questionnaire_version_id = @version_id AND i.item_code = 'P1B-08' AND i.deleted = 0 AND q.deleted = 0;
UPDATE assessment_questionnaire_item
SET required_answer = true, scored = true, weight = 1,
    transfer_category = 'FALSE_FRIEND',
    context_level = 'PHRASE',
    construct_code = 'LEXICAL_TRANSFER',
    target_word = 'sensible',
    option_explanations_json = '{"A":"干扰项 A “理性的”（raisonnable）：英语 sensible 的近义词，是强干扰项。","B":"干扰项 B “不理性的”（irrationnelle）：英语 sensible 的反义词，形成认知对比。","C":"干扰项 C “合乎逻辑的”（logique）：英语 sensible 的语义域成员。","D":"此题为假朋友（同形异义词）。法语 sensible = “敏感的”，英语形似词 sensible = “明智的”。\\n（11、12、13题为一组对应）"}',
    display_condition_json = 'null'
WHERE questionnaire_version_id = @version_id AND item_code = 'P1B-08' AND deleted = 0;
UPDATE assessment_question_version
SET question_type = 'SINGLE_CHOICE',
    stem_text = 'une personne **sensible**',
    prompt_text = '请选出与下列短语中划线单词意思相反的选项',
    options_json = '[{"key":"A","label":"raisonnable"},{"key":"B","label":"irrationnelle"},{"key":"C","label":"logique"},{"key":"D","label":"indifférente"}]',
    correct_answer_json = '["D"]',
    explanation_text = '8. une personne sensible
#题目分析：答案：D
此题为假朋友（同形异义词）。法语 sensible = “敏感的”，英语形似词 sensible = “明智的”。
干扰项 A “理性的”（raisonnable）：英语 sensible 的近义词，是强干扰项。
干扰项 B “不理性的”（irrationnelle）：英语 sensible 的反义词，形成认知对比。
干扰项 C “合乎逻辑的”（logique）：英语 sensible 的语义域成员。
（11、12、13题为一组对应）',
    option_explanations_json = '{"A":"干扰项 A “理性的”（raisonnable）：英语 sensible 的近义词，是强干扰项。","B":"干扰项 B “不理性的”（irrationnelle）：英语 sensible 的反义词，形成认知对比。","C":"干扰项 C “合乎逻辑的”（logique）：英语 sensible 的语义域成员。","D":"此题为假朋友（同形异义词）。法语 sensible = “敏感的”，英语形似词 sensible = “明智的”。\\n（11、12、13题为一组对应）"}',
    required_answer = true, weight = 1,
    transfer_category = 'FALSE_FRIEND',
    context_level = 'PHRASE',
    construct_code = 'LEXICAL_TRANSFER',
    target_word = 'sensible',
    display_condition_json = 'null',
    source_reference = 'questionnaire:P1B-08',
    content_hash = '1d857dd44acce3c0ca87d0bd7eed973ec922b121487fc82d5197409d30ea6a65'
WHERE question_bank_id = @bank_id AND question_code = 'P1B-08' AND version_no = 1 AND deleted = 0;

-- P1B-09
UPDATE assessment_question q
JOIN assessment_questionnaire_item i ON i.assessment_question_id = q.id
SET q.question_type = 'SINGLE_CHOICE',
    q.stem_text = 'une chambre **propre**',
    q.prompt_text = '请选出与下列短语中划线单词意思相反的选项',
    q.options_json = '[{"key":"A","label":"sale"},{"key":"B","label":"inappropriée"},{"key":"C","label":"ordonnée"},{"key":"D","label":"vide"}]',
    q.correct_answer_json = '["A"]',
    q.explanation_text = '9. une chambre propre
#题目分析：答案：A
此题为假朋友（同形异义词）。法语 propre = “干净的”，英语形似词 proper = “适当的”。
干扰项 B “不适当的”（inappropriée）：英语 proper 的反义词，是强干扰项。
干扰项 C “整洁的”（ordonnée）：与 propre 近义，非反义。
干扰项 D “空的”（vide）：语义无关。',
    q.score = 1,
    q.section_code = 'P1B',
    q.required_answer = true,
    q.weight = 1,
    q.transfer_category = 'FALSE_FRIEND',
    q.context_level = 'PHRASE',
    q.construct_code = 'LEXICAL_TRANSFER',
    q.target_word = 'propre',
    q.option_explanations_json = '{"B":"干扰项 B “不适当的”（inappropriée）：英语 proper 的反义词，是强干扰项。","C":"干扰项 C “整洁的”（ordonnée）：与 propre 近义，非反义。","D":"干扰项 D “空的”（vide）：语义无关。","A":"此题为假朋友（同形异义词）。法语 propre = “干净的”，英语形似词 proper = “适当的”。"}',
    q.display_condition_json = 'null'
WHERE i.questionnaire_version_id = @version_id AND i.item_code = 'P1B-09' AND i.deleted = 0 AND q.deleted = 0;
UPDATE assessment_questionnaire_item
SET required_answer = true, scored = true, weight = 1,
    transfer_category = 'FALSE_FRIEND',
    context_level = 'PHRASE',
    construct_code = 'LEXICAL_TRANSFER',
    target_word = 'propre',
    option_explanations_json = '{"B":"干扰项 B “不适当的”（inappropriée）：英语 proper 的反义词，是强干扰项。","C":"干扰项 C “整洁的”（ordonnée）：与 propre 近义，非反义。","D":"干扰项 D “空的”（vide）：语义无关。","A":"此题为假朋友（同形异义词）。法语 propre = “干净的”，英语形似词 proper = “适当的”。"}',
    display_condition_json = 'null'
WHERE questionnaire_version_id = @version_id AND item_code = 'P1B-09' AND deleted = 0;
UPDATE assessment_question_version
SET question_type = 'SINGLE_CHOICE',
    stem_text = 'une chambre **propre**',
    prompt_text = '请选出与下列短语中划线单词意思相反的选项',
    options_json = '[{"key":"A","label":"sale"},{"key":"B","label":"inappropriée"},{"key":"C","label":"ordonnée"},{"key":"D","label":"vide"}]',
    correct_answer_json = '["A"]',
    explanation_text = '9. une chambre propre
#题目分析：答案：A
此题为假朋友（同形异义词）。法语 propre = “干净的”，英语形似词 proper = “适当的”。
干扰项 B “不适当的”（inappropriée）：英语 proper 的反义词，是强干扰项。
干扰项 C “整洁的”（ordonnée）：与 propre 近义，非反义。
干扰项 D “空的”（vide）：语义无关。',
    option_explanations_json = '{"B":"干扰项 B “不适当的”（inappropriée）：英语 proper 的反义词，是强干扰项。","C":"干扰项 C “整洁的”（ordonnée）：与 propre 近义，非反义。","D":"干扰项 D “空的”（vide）：语义无关。","A":"此题为假朋友（同形异义词）。法语 propre = “干净的”，英语形似词 proper = “适当的”。"}',
    required_answer = true, weight = 1,
    transfer_category = 'FALSE_FRIEND',
    context_level = 'PHRASE',
    construct_code = 'LEXICAL_TRANSFER',
    target_word = 'propre',
    display_condition_json = 'null',
    source_reference = 'questionnaire:P1B-09',
    content_hash = '1f6dd1197f952a62769946265acbd9fc5ba1a323701169e84fe567d13b292d12'
WHERE question_bank_id = @bank_id AND question_code = 'P1B-09' AND version_no = 1 AND deleted = 0;

-- P1B-10
UPDATE assessment_question q
JOIN assessment_questionnaire_item i ON i.assessment_question_id = q.id
SET q.question_type = 'SINGLE_CHOICE',
    q.stem_text = 'une réponse **exacte**',
    q.prompt_text = '请选出与下列短语中划线单词意思相反的选项',
    q.options_json = '[{"key":"A","label":"juste"},{"key":"B","label":"fausse"},{"key":"C","label":"précise"},{"key":"D","label":"claire"}]',
    q.correct_answer_json = '["B"]',
    q.explanation_text = '10. une réponse exacte
#题目分析：答案：B
此题为英法同源词。法语 exacte = “准确的”，同源英语 exact = “准确的”。
干扰项 A “正确的”（juste）：近义词，非反义。
干扰项 C “精确的”（précise）：近义词，非反义。
干扰项 D “清晰的”（claire）：语义相关但非反义。
（14、15题为一组对应）',
    q.score = 1,
    q.section_code = 'P1B',
    q.required_answer = true,
    q.weight = 1,
    q.transfer_category = 'COGNATE',
    q.context_level = 'PHRASE',
    q.construct_code = 'LEXICAL_TRANSFER',
    q.target_word = 'exacte',
    q.option_explanations_json = '{"A":"干扰项 A “正确的”（juste）：近义词，非反义。","C":"干扰项 C “精确的”（précise）：近义词，非反义。","D":"干扰项 D “清晰的”（claire）：语义相关但非反义。","B":"此题为英法同源词。法语 exacte = “准确的”，同源英语 exact = “准确的”。\\n（14、15题为一组对应）"}',
    q.display_condition_json = 'null'
WHERE i.questionnaire_version_id = @version_id AND i.item_code = 'P1B-10' AND i.deleted = 0 AND q.deleted = 0;
UPDATE assessment_questionnaire_item
SET required_answer = true, scored = true, weight = 1,
    transfer_category = 'COGNATE',
    context_level = 'PHRASE',
    construct_code = 'LEXICAL_TRANSFER',
    target_word = 'exacte',
    option_explanations_json = '{"A":"干扰项 A “正确的”（juste）：近义词，非反义。","C":"干扰项 C “精确的”（précise）：近义词，非反义。","D":"干扰项 D “清晰的”（claire）：语义相关但非反义。","B":"此题为英法同源词。法语 exacte = “准确的”，同源英语 exact = “准确的”。\\n（14、15题为一组对应）"}',
    display_condition_json = 'null'
WHERE questionnaire_version_id = @version_id AND item_code = 'P1B-10' AND deleted = 0;
UPDATE assessment_question_version
SET question_type = 'SINGLE_CHOICE',
    stem_text = 'une réponse **exacte**',
    prompt_text = '请选出与下列短语中划线单词意思相反的选项',
    options_json = '[{"key":"A","label":"juste"},{"key":"B","label":"fausse"},{"key":"C","label":"précise"},{"key":"D","label":"claire"}]',
    correct_answer_json = '["B"]',
    explanation_text = '10. une réponse exacte
#题目分析：答案：B
此题为英法同源词。法语 exacte = “准确的”，同源英语 exact = “准确的”。
干扰项 A “正确的”（juste）：近义词，非反义。
干扰项 C “精确的”（précise）：近义词，非反义。
干扰项 D “清晰的”（claire）：语义相关但非反义。
（14、15题为一组对应）',
    option_explanations_json = '{"A":"干扰项 A “正确的”（juste）：近义词，非反义。","C":"干扰项 C “精确的”（précise）：近义词，非反义。","D":"干扰项 D “清晰的”（claire）：语义相关但非反义。","B":"此题为英法同源词。法语 exacte = “准确的”，同源英语 exact = “准确的”。\\n（14、15题为一组对应）"}',
    required_answer = true, weight = 1,
    transfer_category = 'COGNATE',
    context_level = 'PHRASE',
    construct_code = 'LEXICAL_TRANSFER',
    target_word = 'exacte',
    display_condition_json = 'null',
    source_reference = 'questionnaire:P1B-10',
    content_hash = '64d64ff32c5be503d7cdc22c4c6a5ff4bb4b3c2b19f66a479ea884b060cfe36c'
WHERE question_bank_id = @bank_id AND question_code = 'P1B-10' AND version_no = 1 AND deleted = 0;

-- P2-01
UPDATE assessment_question q
JOIN assessment_questionnaire_item i ON i.assessment_question_id = q.id
SET q.question_type = 'SINGLE_CHOICE',
    q.stem_text = 'La **location** de cet appartement coûte 400 euros par mois.',
    q.prompt_text = '请根据句子选择画线单词的同义解释',
    q.options_json = '[{"key":"A","label":"vente"},{"key":"B","label":"achat"},{"key":"C","label":"louage"},{"key":"D","label":"endroit"}]',
    q.correct_answer_json = '["C"]',
    q.explanation_text = '1. #题目分析：答案：C
此题为假朋友（同形异义词）。法语 location = “出租 / 租金”，英语形似词 location = “地点 / 位置”。
干扰项 A “出售”（vente）：与“出租”语义相反，但英语 location 无此义。
干扰项 B “购买”（achat）：同样为交易行为，语义域相关。
干扰项 D “地点”（endroit）：英语 location 的直接翻译，是强干扰项。',
    q.score = 1,
    q.section_code = 'P2',
    q.required_answer = true,
    q.weight = 1,
    q.transfer_category = 'FALSE_FRIEND',
    q.context_level = 'SENTENCE',
    q.construct_code = 'CONTEXT_REPAIR',
    q.target_word = 'location',
    q.option_explanations_json = '{"A":"干扰项 A “出售”（vente）：与“出租”语义相反，但英语 location 无此义。","B":"干扰项 B “购买”（achat）：同样为交易行为，语义域相关。","D":"干扰项 D “地点”（endroit）：英语 location 的直接翻译，是强干扰项。","C":"此题为假朋友（同形异义词）。法语 location = “出租 / 租金”，英语形似词 location = “地点 / 位置”。"}',
    q.display_condition_json = 'null'
WHERE i.questionnaire_version_id = @version_id AND i.item_code = 'P2-01' AND i.deleted = 0 AND q.deleted = 0;
UPDATE assessment_questionnaire_item
SET required_answer = true, scored = true, weight = 1,
    transfer_category = 'FALSE_FRIEND',
    context_level = 'SENTENCE',
    construct_code = 'CONTEXT_REPAIR',
    target_word = 'location',
    option_explanations_json = '{"A":"干扰项 A “出售”（vente）：与“出租”语义相反，但英语 location 无此义。","B":"干扰项 B “购买”（achat）：同样为交易行为，语义域相关。","D":"干扰项 D “地点”（endroit）：英语 location 的直接翻译，是强干扰项。","C":"此题为假朋友（同形异义词）。法语 location = “出租 / 租金”，英语形似词 location = “地点 / 位置”。"}',
    display_condition_json = 'null'
WHERE questionnaire_version_id = @version_id AND item_code = 'P2-01' AND deleted = 0;
UPDATE assessment_question_version
SET question_type = 'SINGLE_CHOICE',
    stem_text = 'La **location** de cet appartement coûte 400 euros par mois.',
    prompt_text = '请根据句子选择画线单词的同义解释',
    options_json = '[{"key":"A","label":"vente"},{"key":"B","label":"achat"},{"key":"C","label":"louage"},{"key":"D","label":"endroit"}]',
    correct_answer_json = '["C"]',
    explanation_text = '1. #题目分析：答案：C
此题为假朋友（同形异义词）。法语 location = “出租 / 租金”，英语形似词 location = “地点 / 位置”。
干扰项 A “出售”（vente）：与“出租”语义相反，但英语 location 无此义。
干扰项 B “购买”（achat）：同样为交易行为，语义域相关。
干扰项 D “地点”（endroit）：英语 location 的直接翻译，是强干扰项。',
    option_explanations_json = '{"A":"干扰项 A “出售”（vente）：与“出租”语义相反，但英语 location 无此义。","B":"干扰项 B “购买”（achat）：同样为交易行为，语义域相关。","D":"干扰项 D “地点”（endroit）：英语 location 的直接翻译，是强干扰项。","C":"此题为假朋友（同形异义词）。法语 location = “出租 / 租金”，英语形似词 location = “地点 / 位置”。"}',
    required_answer = true, weight = 1,
    transfer_category = 'FALSE_FRIEND',
    context_level = 'SENTENCE',
    construct_code = 'CONTEXT_REPAIR',
    target_word = 'location',
    display_condition_json = 'null',
    source_reference = 'questionnaire:P2-01',
    content_hash = '1e986972d4f444817ab70484be3ee3c2b8ab3ba8636f6989ff3cd311eaf9e4b9'
WHERE question_bank_id = @bank_id AND question_code = 'P2-01' AND version_no = 1 AND deleted = 0;

-- P2-02
UPDATE assessment_question q
JOIN assessment_questionnaire_item i ON i.assessment_question_id = q.id
SET q.question_type = 'SINGLE_CHOICE',
    q.stem_text = 'Le petit **commerce** local a du mal à survivre.',
    q.prompt_text = '请根据句子选择画线单词的同义解释',
    q.options_json = '[{"key":"A","label":"affaires"},{"key":"B","label":"trafic"},{"key":"C","label":"marché"},{"key":"D","label":"industrie"}]',
    q.correct_answer_json = '["A"]',
    q.explanation_text = '2. #题目分析：答案：A
此题为英法同源词。法语 commerce = “贸易 / 生意”，同源英语 commerce = “商业”。
干扰项 B “交通”（trafic）：词形相似（trafic / commerce），语义域不同。
干扰项 C “市场”（marché）：语义相近（商业活动），但非精确同义。
干扰项 D “工业”（industrie）：经济领域词汇，语义相关但不同。',
    q.score = 1,
    q.section_code = 'P2',
    q.required_answer = true,
    q.weight = 1,
    q.transfer_category = 'COGNATE',
    q.context_level = 'SENTENCE',
    q.construct_code = 'CONTEXT_REPAIR',
    q.target_word = 'commerce',
    q.option_explanations_json = '{"B":"干扰项 B “交通”（trafic）：词形相似（trafic / commerce），语义域不同。","C":"干扰项 C “市场”（marché）：语义相近（商业活动），但非精确同义。","D":"干扰项 D “工业”（industrie）：经济领域词汇，语义相关但不同。","A":"此题为英法同源词。法语 commerce = “贸易 / 生意”，同源英语 commerce = “商业”。"}',
    q.display_condition_json = 'null'
WHERE i.questionnaire_version_id = @version_id AND i.item_code = 'P2-02' AND i.deleted = 0 AND q.deleted = 0;
UPDATE assessment_questionnaire_item
SET required_answer = true, scored = true, weight = 1,
    transfer_category = 'COGNATE',
    context_level = 'SENTENCE',
    construct_code = 'CONTEXT_REPAIR',
    target_word = 'commerce',
    option_explanations_json = '{"B":"干扰项 B “交通”（trafic）：词形相似（trafic / commerce），语义域不同。","C":"干扰项 C “市场”（marché）：语义相近（商业活动），但非精确同义。","D":"干扰项 D “工业”（industrie）：经济领域词汇，语义相关但不同。","A":"此题为英法同源词。法语 commerce = “贸易 / 生意”，同源英语 commerce = “商业”。"}',
    display_condition_json = 'null'
WHERE questionnaire_version_id = @version_id AND item_code = 'P2-02' AND deleted = 0;
UPDATE assessment_question_version
SET question_type = 'SINGLE_CHOICE',
    stem_text = 'Le petit **commerce** local a du mal à survivre.',
    prompt_text = '请根据句子选择画线单词的同义解释',
    options_json = '[{"key":"A","label":"affaires"},{"key":"B","label":"trafic"},{"key":"C","label":"marché"},{"key":"D","label":"industrie"}]',
    correct_answer_json = '["A"]',
    explanation_text = '2. #题目分析：答案：A
此题为英法同源词。法语 commerce = “贸易 / 生意”，同源英语 commerce = “商业”。
干扰项 B “交通”（trafic）：词形相似（trafic / commerce），语义域不同。
干扰项 C “市场”（marché）：语义相近（商业活动），但非精确同义。
干扰项 D “工业”（industrie）：经济领域词汇，语义相关但不同。',
    option_explanations_json = '{"B":"干扰项 B “交通”（trafic）：词形相似（trafic / commerce），语义域不同。","C":"干扰项 C “市场”（marché）：语义相近（商业活动），但非精确同义。","D":"干扰项 D “工业”（industrie）：经济领域词汇，语义相关但不同。","A":"此题为英法同源词。法语 commerce = “贸易 / 生意”，同源英语 commerce = “商业”。"}',
    required_answer = true, weight = 1,
    transfer_category = 'COGNATE',
    context_level = 'SENTENCE',
    construct_code = 'CONTEXT_REPAIR',
    target_word = 'commerce',
    display_condition_json = 'null',
    source_reference = 'questionnaire:P2-02',
    content_hash = '63f247086a5b7f0f5061bcfee5bdae410fc2b0065b749dc116143ffadf9a335b'
WHERE question_bank_id = @bank_id AND question_code = 'P2-02' AND version_no = 1 AND deleted = 0;

-- P2-03
UPDATE assessment_question q
JOIN assessment_questionnaire_item i ON i.assessment_question_id = q.id
SET q.question_type = 'SINGLE_CHOICE',
    q.stem_text = 'Il y a une **peinture** représentant une femme dans cette galerie.',
    q.prompt_text = '请根据句子选择画线单词的同义解释',
    q.options_json = '[{"key":"A","label":"sculpture"},{"key":"B","label":"tableau"},{"key":"C","label":"photo"},{"key":"D","label":"peintre"}]',
    q.correct_answer_json = '["B"]',
    q.explanation_text = '3. #题目分析：答案：B. tableau
此题为纯法语对照词。法语 peinture 在此语境中指“绘画作品/画作”。题干 une peinture représentant une femme 即“一幅描绘女性的画作”。
干扰项 A “雕塑”（sculpture）：同为艺术形式，但与“绘画”媒介不同，属于艺术域内的语义区分干扰。
干扰项 C “照片”（photo）：同为视觉图像，但属于不同创作媒介（绘画 vs 摄影），用于检测学生对“画作”与“图像”的精细区分能力。
干扰项 D “画家”（peintre）：与 peinture 词形高度相似（同为 peint- 开头），且语义关联（画家创作画作）。但题干询问的是“画作”本身，而非创作者，若学生仅凭词形猜测或混淆派生关系，容易误选。',
    q.score = 1,
    q.section_code = 'P2',
    q.required_answer = true,
    q.weight = 1,
    q.transfer_category = 'FRENCH_CONTROL',
    q.context_level = 'SENTENCE',
    q.construct_code = 'CONTEXT_REPAIR',
    q.target_word = 'peinture',
    q.option_explanations_json = '{"A":"干扰项 A “雕塑”（sculpture）：同为艺术形式，但与“绘画”媒介不同，属于艺术域内的语义区分干扰。","C":"干扰项 C “照片”（photo）：同为视觉图像，但属于不同创作媒介（绘画 vs 摄影），用于检测学生对“画作”与“图像”的精细区分能力。","D":"干扰项 D “画家”（peintre）：与 peinture 词形高度相似（同为 peint- 开头），且语义关联（画家创作画作）。但题干询问的是“画作”本身，而非创作者，若学生仅凭词形猜测或混淆派生关系，容易误选。","B":"此题为纯法语对照词。法语 peinture 在此语境中指“绘画作品/画作”。题干 une peinture représentant une femme 即“一幅描绘女性的画作”。"}',
    q.display_condition_json = 'null'
WHERE i.questionnaire_version_id = @version_id AND i.item_code = 'P2-03' AND i.deleted = 0 AND q.deleted = 0;
UPDATE assessment_questionnaire_item
SET required_answer = true, scored = true, weight = 1,
    transfer_category = 'FRENCH_CONTROL',
    context_level = 'SENTENCE',
    construct_code = 'CONTEXT_REPAIR',
    target_word = 'peinture',
    option_explanations_json = '{"A":"干扰项 A “雕塑”（sculpture）：同为艺术形式，但与“绘画”媒介不同，属于艺术域内的语义区分干扰。","C":"干扰项 C “照片”（photo）：同为视觉图像，但属于不同创作媒介（绘画 vs 摄影），用于检测学生对“画作”与“图像”的精细区分能力。","D":"干扰项 D “画家”（peintre）：与 peinture 词形高度相似（同为 peint- 开头），且语义关联（画家创作画作）。但题干询问的是“画作”本身，而非创作者，若学生仅凭词形猜测或混淆派生关系，容易误选。","B":"此题为纯法语对照词。法语 peinture 在此语境中指“绘画作品/画作”。题干 une peinture représentant une femme 即“一幅描绘女性的画作”。"}',
    display_condition_json = 'null'
WHERE questionnaire_version_id = @version_id AND item_code = 'P2-03' AND deleted = 0;
UPDATE assessment_question_version
SET question_type = 'SINGLE_CHOICE',
    stem_text = 'Il y a une **peinture** représentant une femme dans cette galerie.',
    prompt_text = '请根据句子选择画线单词的同义解释',
    options_json = '[{"key":"A","label":"sculpture"},{"key":"B","label":"tableau"},{"key":"C","label":"photo"},{"key":"D","label":"peintre"}]',
    correct_answer_json = '["B"]',
    explanation_text = '3. #题目分析：答案：B. tableau
此题为纯法语对照词。法语 peinture 在此语境中指“绘画作品/画作”。题干 une peinture représentant une femme 即“一幅描绘女性的画作”。
干扰项 A “雕塑”（sculpture）：同为艺术形式，但与“绘画”媒介不同，属于艺术域内的语义区分干扰。
干扰项 C “照片”（photo）：同为视觉图像，但属于不同创作媒介（绘画 vs 摄影），用于检测学生对“画作”与“图像”的精细区分能力。
干扰项 D “画家”（peintre）：与 peinture 词形高度相似（同为 peint- 开头），且语义关联（画家创作画作）。但题干询问的是“画作”本身，而非创作者，若学生仅凭词形猜测或混淆派生关系，容易误选。',
    option_explanations_json = '{"A":"干扰项 A “雕塑”（sculpture）：同为艺术形式，但与“绘画”媒介不同，属于艺术域内的语义区分干扰。","C":"干扰项 C “照片”（photo）：同为视觉图像，但属于不同创作媒介（绘画 vs 摄影），用于检测学生对“画作”与“图像”的精细区分能力。","D":"干扰项 D “画家”（peintre）：与 peinture 词形高度相似（同为 peint- 开头），且语义关联（画家创作画作）。但题干询问的是“画作”本身，而非创作者，若学生仅凭词形猜测或混淆派生关系，容易误选。","B":"此题为纯法语对照词。法语 peinture 在此语境中指“绘画作品/画作”。题干 une peinture représentant une femme 即“一幅描绘女性的画作”。"}',
    required_answer = true, weight = 1,
    transfer_category = 'FRENCH_CONTROL',
    context_level = 'SENTENCE',
    construct_code = 'CONTEXT_REPAIR',
    target_word = 'peinture',
    display_condition_json = 'null',
    source_reference = 'questionnaire:P2-03',
    content_hash = 'ec0f0119baf6f9b35c2b52b56c4cb5736b62016187f184acf82e2bf6090246ff'
WHERE question_bank_id = @bank_id AND question_code = 'P2-03' AND version_no = 1 AND deleted = 0;

-- P2-04
UPDATE assessment_question q
JOIN assessment_questionnaire_item i ON i.assessment_question_id = q.id
SET q.question_type = 'SINGLE_CHOICE',
    q.stem_text = 'Il **reporte** sa décision à un moment plus favorable.',
    q.prompt_text = '请根据句子选择画线单词的同义解释',
    q.options_json = '[{"key":"A","label":"rapporter"},{"key":"B","label":"refuser"},{"key":"C","label":"référer"},{"key":"D","label":"retarder"}]',
    q.correct_answer_json = '["D"]',
    q.explanation_text = '#题目分析：答案：D
此题为假朋友（同形异义词）。法语 reporter = “推迟”，英语形似词 report = “报道 / 报告”。
干扰项 A “带回 / 报告”（rapporter）：与 reporter 形近，且部分语义重叠（“带回”）。
干扰项 B “拒绝”（refuser）：词形部分相似，混淆选项
干扰项 C “参考 / 提及”（référer）：词形部分相似，语义无关。',
    q.score = 1,
    q.section_code = 'P2',
    q.required_answer = true,
    q.weight = 1,
    q.transfer_category = 'FALSE_FRIEND',
    q.context_level = 'SENTENCE',
    q.construct_code = 'CONTEXT_REPAIR',
    q.target_word = 'reporter',
    q.option_explanations_json = '{"A":"干扰项 A “带回 / 报告”（rapporter）：与 reporter 形近，且部分语义重叠（“带回”）。","B":"干扰项 B “拒绝”（refuser）：词形部分相似，混淆选项","C":"干扰项 C “参考 / 提及”（référer）：词形部分相似，语义无关。","D":"此题为假朋友（同形异义词）。法语 reporter = “推迟”，英语形似词 report = “报道 / 报告”。"}',
    q.display_condition_json = 'null'
WHERE i.questionnaire_version_id = @version_id AND i.item_code = 'P2-04' AND i.deleted = 0 AND q.deleted = 0;
UPDATE assessment_questionnaire_item
SET required_answer = true, scored = true, weight = 1,
    transfer_category = 'FALSE_FRIEND',
    context_level = 'SENTENCE',
    construct_code = 'CONTEXT_REPAIR',
    target_word = 'reporter',
    option_explanations_json = '{"A":"干扰项 A “带回 / 报告”（rapporter）：与 reporter 形近，且部分语义重叠（“带回”）。","B":"干扰项 B “拒绝”（refuser）：词形部分相似，混淆选项","C":"干扰项 C “参考 / 提及”（référer）：词形部分相似，语义无关。","D":"此题为假朋友（同形异义词）。法语 reporter = “推迟”，英语形似词 report = “报道 / 报告”。"}',
    display_condition_json = 'null'
WHERE questionnaire_version_id = @version_id AND item_code = 'P2-04' AND deleted = 0;
UPDATE assessment_question_version
SET question_type = 'SINGLE_CHOICE',
    stem_text = 'Il **reporte** sa décision à un moment plus favorable.',
    prompt_text = '请根据句子选择画线单词的同义解释',
    options_json = '[{"key":"A","label":"rapporter"},{"key":"B","label":"refuser"},{"key":"C","label":"référer"},{"key":"D","label":"retarder"}]',
    correct_answer_json = '["D"]',
    explanation_text = '#题目分析：答案：D
此题为假朋友（同形异义词）。法语 reporter = “推迟”，英语形似词 report = “报道 / 报告”。
干扰项 A “带回 / 报告”（rapporter）：与 reporter 形近，且部分语义重叠（“带回”）。
干扰项 B “拒绝”（refuser）：词形部分相似，混淆选项
干扰项 C “参考 / 提及”（référer）：词形部分相似，语义无关。',
    option_explanations_json = '{"A":"干扰项 A “带回 / 报告”（rapporter）：与 reporter 形近，且部分语义重叠（“带回”）。","B":"干扰项 B “拒绝”（refuser）：词形部分相似，混淆选项","C":"干扰项 C “参考 / 提及”（référer）：词形部分相似，语义无关。","D":"此题为假朋友（同形异义词）。法语 reporter = “推迟”，英语形似词 report = “报道 / 报告”。"}',
    required_answer = true, weight = 1,
    transfer_category = 'FALSE_FRIEND',
    context_level = 'SENTENCE',
    construct_code = 'CONTEXT_REPAIR',
    target_word = 'reporter',
    display_condition_json = 'null',
    source_reference = 'questionnaire:P2-04',
    content_hash = 'f04bd6d84f9c00be6ec8b4d4f6b6b18cdf29aef884c4aed70c2a76da8bc11395'
WHERE question_bank_id = @bank_id AND question_code = 'P2-04' AND version_no = 1 AND deleted = 0;

-- P2-05
UPDATE assessment_question q
JOIN assessment_questionnaire_item i ON i.assessment_question_id = q.id
SET q.question_type = 'SINGLE_CHOICE',
    q.stem_text = 'Les hirondelles **annoncent** le printemps.',
    q.prompt_text = '请根据句子选择画线单词的同义解释',
    q.options_json = '[{"key":"A","label":"marquer"},{"key":"B","label":"cacher"},{"key":"C","label":"informer"},{"key":"D","label":"célébrer"}]',
    q.correct_answer_json = '["C"]',
    q.explanation_text = '5. Les hirondelles annoncent le printemps
#题目分析：答案：C
此题为英法同源词。法语 annoncer = “宣布 / 告知”，同源英语 announce = “宣布”。
干扰项 A “标记”（marquer）：语义相近（“标志着”），是强干扰项。
干扰项 B “隐藏”（cacher）：语义相反。
干扰项 D “庆祝”（célébrer）：语义相关但不同（宣布 ≠ 庆祝）。',
    q.score = 1,
    q.section_code = 'P2',
    q.required_answer = true,
    q.weight = 1,
    q.transfer_category = 'COGNATE',
    q.context_level = 'SENTENCE',
    q.construct_code = 'CONTEXT_REPAIR',
    q.target_word = 'annoncer',
    q.option_explanations_json = '{"A":"干扰项 A “标记”（marquer）：语义相近（“标志着”），是强干扰项。","B":"干扰项 B “隐藏”（cacher）：语义相反。","D":"干扰项 D “庆祝”（célébrer）：语义相关但不同（宣布 ≠ 庆祝）。","C":"此题为英法同源词。法语 annoncer = “宣布 / 告知”，同源英语 announce = “宣布”。"}',
    q.display_condition_json = 'null'
WHERE i.questionnaire_version_id = @version_id AND i.item_code = 'P2-05' AND i.deleted = 0 AND q.deleted = 0;
UPDATE assessment_questionnaire_item
SET required_answer = true, scored = true, weight = 1,
    transfer_category = 'COGNATE',
    context_level = 'SENTENCE',
    construct_code = 'CONTEXT_REPAIR',
    target_word = 'annoncer',
    option_explanations_json = '{"A":"干扰项 A “标记”（marquer）：语义相近（“标志着”），是强干扰项。","B":"干扰项 B “隐藏”（cacher）：语义相反。","D":"干扰项 D “庆祝”（célébrer）：语义相关但不同（宣布 ≠ 庆祝）。","C":"此题为英法同源词。法语 annoncer = “宣布 / 告知”，同源英语 announce = “宣布”。"}',
    display_condition_json = 'null'
WHERE questionnaire_version_id = @version_id AND item_code = 'P2-05' AND deleted = 0;
UPDATE assessment_question_version
SET question_type = 'SINGLE_CHOICE',
    stem_text = 'Les hirondelles **annoncent** le printemps.',
    prompt_text = '请根据句子选择画线单词的同义解释',
    options_json = '[{"key":"A","label":"marquer"},{"key":"B","label":"cacher"},{"key":"C","label":"informer"},{"key":"D","label":"célébrer"}]',
    correct_answer_json = '["C"]',
    explanation_text = '5. Les hirondelles annoncent le printemps
#题目分析：答案：C
此题为英法同源词。法语 annoncer = “宣布 / 告知”，同源英语 announce = “宣布”。
干扰项 A “标记”（marquer）：语义相近（“标志着”），是强干扰项。
干扰项 B “隐藏”（cacher）：语义相反。
干扰项 D “庆祝”（célébrer）：语义相关但不同（宣布 ≠ 庆祝）。',
    option_explanations_json = '{"A":"干扰项 A “标记”（marquer）：语义相近（“标志着”），是强干扰项。","B":"干扰项 B “隐藏”（cacher）：语义相反。","D":"干扰项 D “庆祝”（célébrer）：语义相关但不同（宣布 ≠ 庆祝）。","C":"此题为英法同源词。法语 annoncer = “宣布 / 告知”，同源英语 announce = “宣布”。"}',
    required_answer = true, weight = 1,
    transfer_category = 'COGNATE',
    context_level = 'SENTENCE',
    construct_code = 'CONTEXT_REPAIR',
    target_word = 'annoncer',
    display_condition_json = 'null',
    source_reference = 'questionnaire:P2-05',
    content_hash = '5db19009284fe059f37605eb0a72f4ac43ae92879195dea3d02188dcaf06851e'
WHERE question_bank_id = @bank_id AND question_code = 'P2-05' AND version_no = 1 AND deleted = 0;

-- P2-06
UPDATE assessment_question q
JOIN assessment_questionnaire_item i ON i.assessment_question_id = q.id
SET q.question_type = 'SINGLE_CHOICE',
    q.stem_text = 'Les chasseurs ont réussi à **capturer** un animal sauvage.',
    q.prompt_text = '请根据句子选择画线单词的同义解释',
    q.options_json = '[{"key":"A","label":"libérer"},{"key":"B","label":"attraper"},{"key":"C","label":"tuer"},{"key":"D","label":"nourrir"}]',
    q.correct_answer_json = '["B"]',
    q.explanation_text = '6. #题目分析：答案：B
此题为英法同源词。法语 capturer = “捕捉”，同源英语 capture = “捕捉”。
干扰项 A “释放”（libérer）：语义相反。
干扰项 C “杀死”（tuer）：捕捉后可能发生的行为，语义域相关但不同。
干扰项 D “喂养”（nourrir）：捕捉后可能发生的行为，用于检测语义联想过度。',
    q.score = 1,
    q.section_code = 'P2',
    q.required_answer = true,
    q.weight = 1,
    q.transfer_category = 'COGNATE',
    q.context_level = 'SENTENCE',
    q.construct_code = 'CONTEXT_REPAIR',
    q.target_word = 'capturer',
    q.option_explanations_json = '{"A":"干扰项 A “释放”（libérer）：语义相反。","C":"干扰项 C “杀死”（tuer）：捕捉后可能发生的行为，语义域相关但不同。","D":"干扰项 D “喂养”（nourrir）：捕捉后可能发生的行为，用于检测语义联想过度。","B":"此题为英法同源词。法语 capturer = “捕捉”，同源英语 capture = “捕捉”。"}',
    q.display_condition_json = 'null'
WHERE i.questionnaire_version_id = @version_id AND i.item_code = 'P2-06' AND i.deleted = 0 AND q.deleted = 0;
UPDATE assessment_questionnaire_item
SET required_answer = true, scored = true, weight = 1,
    transfer_category = 'COGNATE',
    context_level = 'SENTENCE',
    construct_code = 'CONTEXT_REPAIR',
    target_word = 'capturer',
    option_explanations_json = '{"A":"干扰项 A “释放”（libérer）：语义相反。","C":"干扰项 C “杀死”（tuer）：捕捉后可能发生的行为，语义域相关但不同。","D":"干扰项 D “喂养”（nourrir）：捕捉后可能发生的行为，用于检测语义联想过度。","B":"此题为英法同源词。法语 capturer = “捕捉”，同源英语 capture = “捕捉”。"}',
    display_condition_json = 'null'
WHERE questionnaire_version_id = @version_id AND item_code = 'P2-06' AND deleted = 0;
UPDATE assessment_question_version
SET question_type = 'SINGLE_CHOICE',
    stem_text = 'Les chasseurs ont réussi à **capturer** un animal sauvage.',
    prompt_text = '请根据句子选择画线单词的同义解释',
    options_json = '[{"key":"A","label":"libérer"},{"key":"B","label":"attraper"},{"key":"C","label":"tuer"},{"key":"D","label":"nourrir"}]',
    correct_answer_json = '["B"]',
    explanation_text = '6. #题目分析：答案：B
此题为英法同源词。法语 capturer = “捕捉”，同源英语 capture = “捕捉”。
干扰项 A “释放”（libérer）：语义相反。
干扰项 C “杀死”（tuer）：捕捉后可能发生的行为，语义域相关但不同。
干扰项 D “喂养”（nourrir）：捕捉后可能发生的行为，用于检测语义联想过度。',
    option_explanations_json = '{"A":"干扰项 A “释放”（libérer）：语义相反。","C":"干扰项 C “杀死”（tuer）：捕捉后可能发生的行为，语义域相关但不同。","D":"干扰项 D “喂养”（nourrir）：捕捉后可能发生的行为，用于检测语义联想过度。","B":"此题为英法同源词。法语 capturer = “捕捉”，同源英语 capture = “捕捉”。"}',
    required_answer = true, weight = 1,
    transfer_category = 'COGNATE',
    context_level = 'SENTENCE',
    construct_code = 'CONTEXT_REPAIR',
    target_word = 'capturer',
    display_condition_json = 'null',
    source_reference = 'questionnaire:P2-06',
    content_hash = 'af68b6b73f2a9ce0e27b53c4baa59bcf9d27305fa01e5fc434859a988506649e'
WHERE question_bank_id = @bank_id AND question_code = 'P2-06' AND version_no = 1 AND deleted = 0;

-- P2-07
UPDATE assessment_question q
JOIN assessment_questionnaire_item i ON i.assessment_question_id = q.id
SET q.question_type = 'SINGLE_CHOICE',
    q.stem_text = 'Chaque matin, il doit **se dépêcher** au travail pour ne pas être en retard.',
    q.prompt_text = '请根据句子选择画线单词的同义解释',
    q.options_json = '[{"key":"A","label":"arriver"},{"key":"B","label":"ralentir"},{"key":"C","label":"se hâter"},{"key":"D","label":"attendre"}]',
    q.correct_answer_json = '["C"]',
    q.explanation_text = '7. #题目分析：答案：C
此题为纯法语对照词。法语 se dépêcher = “赶快”，英语无直接同源词。
干扰项 A “到达”（arriver）：语义无关，但动作上“赶快”常为了“到达”，易产生联想。
干扰项 B “放慢”（ralentir）：语义相反。
干扰项 D “等待”（attendre）：语义相反，且与“赶快”形成行为对立。',
    q.score = 1,
    q.section_code = 'P2',
    q.required_answer = true,
    q.weight = 1,
    q.transfer_category = 'COGNATE',
    q.context_level = 'SENTENCE',
    q.construct_code = 'CONTEXT_REPAIR',
    q.target_word = 'se dépêcher',
    q.option_explanations_json = '{"A":"干扰项 A “到达”（arriver）：语义无关，但动作上“赶快”常为了“到达”，易产生联想。","B":"干扰项 B “放慢”（ralentir）：语义相反。","D":"干扰项 D “等待”（attendre）：语义相反，且与“赶快”形成行为对立。","C":"此题为纯法语对照词。法语 se dépêcher = “赶快”，英语无直接同源词。"}',
    q.display_condition_json = 'null'
WHERE i.questionnaire_version_id = @version_id AND i.item_code = 'P2-07' AND i.deleted = 0 AND q.deleted = 0;
UPDATE assessment_questionnaire_item
SET required_answer = true, scored = true, weight = 1,
    transfer_category = 'COGNATE',
    context_level = 'SENTENCE',
    construct_code = 'CONTEXT_REPAIR',
    target_word = 'se dépêcher',
    option_explanations_json = '{"A":"干扰项 A “到达”（arriver）：语义无关，但动作上“赶快”常为了“到达”，易产生联想。","B":"干扰项 B “放慢”（ralentir）：语义相反。","D":"干扰项 D “等待”（attendre）：语义相反，且与“赶快”形成行为对立。","C":"此题为纯法语对照词。法语 se dépêcher = “赶快”，英语无直接同源词。"}',
    display_condition_json = 'null'
WHERE questionnaire_version_id = @version_id AND item_code = 'P2-07' AND deleted = 0;
UPDATE assessment_question_version
SET question_type = 'SINGLE_CHOICE',
    stem_text = 'Chaque matin, il doit **se dépêcher** au travail pour ne pas être en retard.',
    prompt_text = '请根据句子选择画线单词的同义解释',
    options_json = '[{"key":"A","label":"arriver"},{"key":"B","label":"ralentir"},{"key":"C","label":"se hâter"},{"key":"D","label":"attendre"}]',
    correct_answer_json = '["C"]',
    explanation_text = '7. #题目分析：答案：C
此题为纯法语对照词。法语 se dépêcher = “赶快”，英语无直接同源词。
干扰项 A “到达”（arriver）：语义无关，但动作上“赶快”常为了“到达”，易产生联想。
干扰项 B “放慢”（ralentir）：语义相反。
干扰项 D “等待”（attendre）：语义相反，且与“赶快”形成行为对立。',
    option_explanations_json = '{"A":"干扰项 A “到达”（arriver）：语义无关，但动作上“赶快”常为了“到达”，易产生联想。","B":"干扰项 B “放慢”（ralentir）：语义相反。","D":"干扰项 D “等待”（attendre）：语义相反，且与“赶快”形成行为对立。","C":"此题为纯法语对照词。法语 se dépêcher = “赶快”，英语无直接同源词。"}',
    required_answer = true, weight = 1,
    transfer_category = 'COGNATE',
    context_level = 'SENTENCE',
    construct_code = 'CONTEXT_REPAIR',
    target_word = 'se dépêcher',
    display_condition_json = 'null',
    source_reference = 'questionnaire:P2-07',
    content_hash = 'fc31058fe35dd6662c23c4845f181489ee11c06bc8dcaee6a0b751baaa5f6b10'
WHERE question_bank_id = @bank_id AND question_code = 'P2-07' AND version_no = 1 AND deleted = 0;

-- P2-08
UPDATE assessment_question q
JOIN assessment_questionnaire_item i ON i.assessment_question_id = q.id
SET q.question_type = 'SINGLE_CHOICE',
    q.stem_text = 'Marie Curie était une grande **physicienne**.',
    q.prompt_text = '请根据句子选择画线单词的同义解释',
    q.options_json = '[{"key":"A","label":"médecin"},{"key":"B","label":"scientifique en physique"},{"key":"C","label":"chirurgien"},{"key":"D","label":"pharmacien"}]',
    q.correct_answer_json = '["B"]',
    q.explanation_text = '8. #题目分析：答案：B
此题为假朋友（同形异义词）。法语 physicien = “物理学家”，英语形似词 physician = “医生”。
干扰项 A “医生”（médecin）：英语 physician 的直接翻译，是强干扰项（负迁移）。
干扰项 C “外科医生”（chirurgien）：医生的一种，进一步细化干扰。
干扰项 D “药剂师”（pharmacien）：医疗行业相关职业，与 physician 语义域重叠。',
    q.score = 1,
    q.section_code = 'P2',
    q.required_answer = true,
    q.weight = 1,
    q.transfer_category = 'FALSE_FRIEND',
    q.context_level = 'SENTENCE',
    q.construct_code = 'CONTEXT_REPAIR',
    q.target_word = 'physicien',
    q.option_explanations_json = '{"A":"干扰项 A “医生”（médecin）：英语 physician 的直接翻译，是强干扰项（负迁移）。","C":"干扰项 C “外科医生”（chirurgien）：医生的一种，进一步细化干扰。","D":"干扰项 D “药剂师”（pharmacien）：医疗行业相关职业，与 physician 语义域重叠。","B":"此题为假朋友（同形异义词）。法语 physicien = “物理学家”，英语形似词 physician = “医生”。"}',
    q.display_condition_json = 'null'
WHERE i.questionnaire_version_id = @version_id AND i.item_code = 'P2-08' AND i.deleted = 0 AND q.deleted = 0;
UPDATE assessment_questionnaire_item
SET required_answer = true, scored = true, weight = 1,
    transfer_category = 'FALSE_FRIEND',
    context_level = 'SENTENCE',
    construct_code = 'CONTEXT_REPAIR',
    target_word = 'physicien',
    option_explanations_json = '{"A":"干扰项 A “医生”（médecin）：英语 physician 的直接翻译，是强干扰项（负迁移）。","C":"干扰项 C “外科医生”（chirurgien）：医生的一种，进一步细化干扰。","D":"干扰项 D “药剂师”（pharmacien）：医疗行业相关职业，与 physician 语义域重叠。","B":"此题为假朋友（同形异义词）。法语 physicien = “物理学家”，英语形似词 physician = “医生”。"}',
    display_condition_json = 'null'
WHERE questionnaire_version_id = @version_id AND item_code = 'P2-08' AND deleted = 0;
UPDATE assessment_question_version
SET question_type = 'SINGLE_CHOICE',
    stem_text = 'Marie Curie était une grande **physicienne**.',
    prompt_text = '请根据句子选择画线单词的同义解释',
    options_json = '[{"key":"A","label":"médecin"},{"key":"B","label":"scientifique en physique"},{"key":"C","label":"chirurgien"},{"key":"D","label":"pharmacien"}]',
    correct_answer_json = '["B"]',
    explanation_text = '8. #题目分析：答案：B
此题为假朋友（同形异义词）。法语 physicien = “物理学家”，英语形似词 physician = “医生”。
干扰项 A “医生”（médecin）：英语 physician 的直接翻译，是强干扰项（负迁移）。
干扰项 C “外科医生”（chirurgien）：医生的一种，进一步细化干扰。
干扰项 D “药剂师”（pharmacien）：医疗行业相关职业，与 physician 语义域重叠。',
    option_explanations_json = '{"A":"干扰项 A “医生”（médecin）：英语 physician 的直接翻译，是强干扰项（负迁移）。","C":"干扰项 C “外科医生”（chirurgien）：医生的一种，进一步细化干扰。","D":"干扰项 D “药剂师”（pharmacien）：医疗行业相关职业，与 physician 语义域重叠。","B":"此题为假朋友（同形异义词）。法语 physicien = “物理学家”，英语形似词 physician = “医生”。"}',
    required_answer = true, weight = 1,
    transfer_category = 'FALSE_FRIEND',
    context_level = 'SENTENCE',
    construct_code = 'CONTEXT_REPAIR',
    target_word = 'physicien',
    display_condition_json = 'null',
    source_reference = 'questionnaire:P2-08',
    content_hash = '171242a51fcaa5f9e68ae01cfd9807a3535089739ac9ccd7746840b93baf1773'
WHERE question_bank_id = @bank_id AND question_code = 'P2-08' AND version_no = 1 AND deleted = 0;

-- P2-09
UPDATE assessment_question q
JOIN assessment_questionnaire_item i ON i.assessment_question_id = q.id
SET q.question_type = 'SINGLE_CHOICE',
    q.stem_text = 'Ils se sont mêlés à une **sale** affaire.',
    q.prompt_text = '请根据句子选择画线单词的同义解释',
    q.options_json = '[{"key":"A","label":"mauvaise histoire"},{"key":"B","label":"promotion commerciale"},{"key":"C","label":"bonne nouvelle"},{"key":"D","label":"chose propre"}]',
    q.correct_answer_json = '["A"]',
    q.explanation_text = '9. #题目分析：答案：A
此题为假朋友（同形异义词）。法语 sale = “肮脏的 / 坏的 / 卑鄙的”，英语形似词 sale = “促销 / 销售”。题干 une sale affaire 意为“一件坏事 / 丑闻”。
干扰项 B “商业促销”（une promotion commerciale）：英语 sale 的直接翻译，是强干扰项（负迁移）。
干扰项 C “好消息”（une bonne nouvelle）：语义相反。
干扰项 D “干净的东西”（une chose propre）：利用 sale 的本义“肮脏的”与 propre（干净的）形成反义对比，但语义域偏离“事件”范畴，用于检测语义匹配能力。',
    q.score = 1,
    q.section_code = 'P2',
    q.required_answer = true,
    q.weight = 1,
    q.transfer_category = 'FALSE_FRIEND',
    q.context_level = 'SENTENCE',
    q.construct_code = 'CONTEXT_REPAIR',
    q.target_word = 'sale',
    q.option_explanations_json = '{"B":"干扰项 B “商业促销”（une promotion commerciale）：英语 sale 的直接翻译，是强干扰项（负迁移）。","C":"干扰项 C “好消息”（une bonne nouvelle）：语义相反。","D":"干扰项 D “干净的东西”（une chose propre）：利用 sale 的本义“肮脏的”与 propre（干净的）形成反义对比，但语义域偏离“事件”范畴，用于检测语义匹配能力。","A":"此题为假朋友（同形异义词）。法语 sale = “肮脏的 / 坏的 / 卑鄙的”，英语形似词 sale = “促销 / 销售”。题干 une sale affaire 意为“一件坏事 / 丑闻”。"}',
    q.display_condition_json = 'null'
WHERE i.questionnaire_version_id = @version_id AND i.item_code = 'P2-09' AND i.deleted = 0 AND q.deleted = 0;
UPDATE assessment_questionnaire_item
SET required_answer = true, scored = true, weight = 1,
    transfer_category = 'FALSE_FRIEND',
    context_level = 'SENTENCE',
    construct_code = 'CONTEXT_REPAIR',
    target_word = 'sale',
    option_explanations_json = '{"B":"干扰项 B “商业促销”（une promotion commerciale）：英语 sale 的直接翻译，是强干扰项（负迁移）。","C":"干扰项 C “好消息”（une bonne nouvelle）：语义相反。","D":"干扰项 D “干净的东西”（une chose propre）：利用 sale 的本义“肮脏的”与 propre（干净的）形成反义对比，但语义域偏离“事件”范畴，用于检测语义匹配能力。","A":"此题为假朋友（同形异义词）。法语 sale = “肮脏的 / 坏的 / 卑鄙的”，英语形似词 sale = “促销 / 销售”。题干 une sale affaire 意为“一件坏事 / 丑闻”。"}',
    display_condition_json = 'null'
WHERE questionnaire_version_id = @version_id AND item_code = 'P2-09' AND deleted = 0;
UPDATE assessment_question_version
SET question_type = 'SINGLE_CHOICE',
    stem_text = 'Ils se sont mêlés à une **sale** affaire.',
    prompt_text = '请根据句子选择画线单词的同义解释',
    options_json = '[{"key":"A","label":"mauvaise histoire"},{"key":"B","label":"promotion commerciale"},{"key":"C","label":"bonne nouvelle"},{"key":"D","label":"chose propre"}]',
    correct_answer_json = '["A"]',
    explanation_text = '9. #题目分析：答案：A
此题为假朋友（同形异义词）。法语 sale = “肮脏的 / 坏的 / 卑鄙的”，英语形似词 sale = “促销 / 销售”。题干 une sale affaire 意为“一件坏事 / 丑闻”。
干扰项 B “商业促销”（une promotion commerciale）：英语 sale 的直接翻译，是强干扰项（负迁移）。
干扰项 C “好消息”（une bonne nouvelle）：语义相反。
干扰项 D “干净的东西”（une chose propre）：利用 sale 的本义“肮脏的”与 propre（干净的）形成反义对比，但语义域偏离“事件”范畴，用于检测语义匹配能力。',
    option_explanations_json = '{"B":"干扰项 B “商业促销”（une promotion commerciale）：英语 sale 的直接翻译，是强干扰项（负迁移）。","C":"干扰项 C “好消息”（une bonne nouvelle）：语义相反。","D":"干扰项 D “干净的东西”（une chose propre）：利用 sale 的本义“肮脏的”与 propre（干净的）形成反义对比，但语义域偏离“事件”范畴，用于检测语义匹配能力。","A":"此题为假朋友（同形异义词）。法语 sale = “肮脏的 / 坏的 / 卑鄙的”，英语形似词 sale = “促销 / 销售”。题干 une sale affaire 意为“一件坏事 / 丑闻”。"}',
    required_answer = true, weight = 1,
    transfer_category = 'FALSE_FRIEND',
    context_level = 'SENTENCE',
    construct_code = 'CONTEXT_REPAIR',
    target_word = 'sale',
    display_condition_json = 'null',
    source_reference = 'questionnaire:P2-09',
    content_hash = 'c77a10c3c52343aae9ae3674df0c7bbe7794efde5ee61c34c9ba6b51dab2981c'
WHERE question_bank_id = @bank_id AND question_code = 'P2-09' AND version_no = 1 AND deleted = 0;

-- P2-10
UPDATE assessment_question q
JOIN assessment_questionnaire_item i ON i.assessment_question_id = q.id
SET q.question_type = 'SINGLE_CHOICE',
    q.stem_text = 'L’avocat doit **constituer** un dossier pour son client.',
    q.prompt_text = '请根据句子选择画线单词的同义解释',
    q.options_json = '[{"key":"A","label":"détruire"},{"key":"B","label":"former"},{"key":"C","label":"instituer"},{"key":"D","label":"contribuer"}]',
    q.correct_answer_json = '["B"]',
    q.explanation_text = '10. #题目分析：答案：B
此题为英法同源词。法语 constituer = “构成/组成/建立”，同源英语 constitute = “构成 / 组成”。
干扰项 A “破坏”（détruire）：语义相反。
干扰项 C “设立 / 建立”（instituer）：与 constituer 前缀不同（in- vs con-），但词干相近（stituer），语义部分重叠（“建立”），易混淆。
干扰项 D “贡献”（contribuer）：前缀相同（con-），词形高度相似（contribuer / constituer），但语义完全不同，是强干扰项。',
    q.score = 1,
    q.section_code = 'P2',
    q.required_answer = true,
    q.weight = 1,
    q.transfer_category = 'COGNATE',
    q.context_level = 'SENTENCE',
    q.construct_code = 'CONTEXT_REPAIR',
    q.target_word = 'constituer',
    q.option_explanations_json = '{"A":"干扰项 A “破坏”（détruire）：语义相反。","C":"干扰项 C “设立 / 建立”（instituer）：与 constituer 前缀不同（in- vs con-），但词干相近（stituer），语义部分重叠（“建立”），易混淆。","D":"干扰项 D “贡献”（contribuer）：前缀相同（con-），词形高度相似（contribuer / constituer），但语义完全不同，是强干扰项。","B":"此题为英法同源词。法语 constituer = “构成/组成/建立”，同源英语 constitute = “构成 / 组成”。"}',
    q.display_condition_json = 'null'
WHERE i.questionnaire_version_id = @version_id AND i.item_code = 'P2-10' AND i.deleted = 0 AND q.deleted = 0;
UPDATE assessment_questionnaire_item
SET required_answer = true, scored = true, weight = 1,
    transfer_category = 'COGNATE',
    context_level = 'SENTENCE',
    construct_code = 'CONTEXT_REPAIR',
    target_word = 'constituer',
    option_explanations_json = '{"A":"干扰项 A “破坏”（détruire）：语义相反。","C":"干扰项 C “设立 / 建立”（instituer）：与 constituer 前缀不同（in- vs con-），但词干相近（stituer），语义部分重叠（“建立”），易混淆。","D":"干扰项 D “贡献”（contribuer）：前缀相同（con-），词形高度相似（contribuer / constituer），但语义完全不同，是强干扰项。","B":"此题为英法同源词。法语 constituer = “构成/组成/建立”，同源英语 constitute = “构成 / 组成”。"}',
    display_condition_json = 'null'
WHERE questionnaire_version_id = @version_id AND item_code = 'P2-10' AND deleted = 0;
UPDATE assessment_question_version
SET question_type = 'SINGLE_CHOICE',
    stem_text = 'L’avocat doit **constituer** un dossier pour son client.',
    prompt_text = '请根据句子选择画线单词的同义解释',
    options_json = '[{"key":"A","label":"détruire"},{"key":"B","label":"former"},{"key":"C","label":"instituer"},{"key":"D","label":"contribuer"}]',
    correct_answer_json = '["B"]',
    explanation_text = '10. #题目分析：答案：B
此题为英法同源词。法语 constituer = “构成/组成/建立”，同源英语 constitute = “构成 / 组成”。
干扰项 A “破坏”（détruire）：语义相反。
干扰项 C “设立 / 建立”（instituer）：与 constituer 前缀不同（in- vs con-），但词干相近（stituer），语义部分重叠（“建立”），易混淆。
干扰项 D “贡献”（contribuer）：前缀相同（con-），词形高度相似（contribuer / constituer），但语义完全不同，是强干扰项。',
    option_explanations_json = '{"A":"干扰项 A “破坏”（détruire）：语义相反。","C":"干扰项 C “设立 / 建立”（instituer）：与 constituer 前缀不同（in- vs con-），但词干相近（stituer），语义部分重叠（“建立”），易混淆。","D":"干扰项 D “贡献”（contribuer）：前缀相同（con-），词形高度相似（contribuer / constituer），但语义完全不同，是强干扰项。","B":"此题为英法同源词。法语 constituer = “构成/组成/建立”，同源英语 constitute = “构成 / 组成”。"}',
    required_answer = true, weight = 1,
    transfer_category = 'COGNATE',
    context_level = 'SENTENCE',
    construct_code = 'CONTEXT_REPAIR',
    target_word = 'constituer',
    display_condition_json = 'null',
    source_reference = 'questionnaire:P2-10',
    content_hash = '3166e72ffaf95ece81dd2e73ca5dd844649de52e840883727c7b58f8bcd540cf'
WHERE question_bank_id = @bank_id AND question_code = 'P2-10' AND version_no = 1 AND deleted = 0;

-- P3-01
UPDATE assessment_question q
JOIN assessment_questionnaire_item i ON i.assessment_question_id = q.id
SET q.question_type = 'SINGLE_CHOICE',
    q.stem_text = 'Une journée de travail — blanc (1)',
    q.prompt_text = '请阅读上面的完整短文，为空格 (1) 选择最佳答案。',
    q.options_json = '[{"key":"A","label":"piste"},{"key":"B","label":"place"},{"key":"C","label":"chemin"},{"key":"D","label":"champ"}]',
    q.correct_answer_json = '["C"]',
    q.explanation_text = '1. 答案：C. chemin
#题目分析：答案：C
此题为对照词。法语 chemin = “道路 / 路径”，英语无直接同源词。
干扰项 A “小路 / 跑道”（piste）：与 chemin 同属“道路”语义场，但 piste 更偏向非铺装小路或特定用途路线，是强干扰项。
干扰项 B “广场 / 位置”（place）：与“路径”语义部分重叠，但不符合“去上班所走的路线”这一语境。
干扰项 D “田野 / 场地”（champ）：属于空间概念，但与日常通勤路径无关，用于检测语义区分能力。',
    q.score = 1,
    q.section_code = 'P3',
    q.required_answer = true,
    q.weight = 1,
    q.transfer_category = 'COGNATE',
    q.context_level = 'CLOZE',
    q.construct_code = 'CONTEXT_REPAIR',
    q.target_word = 'chemin',
    q.option_explanations_json = '{"A":"干扰项 A “小路 / 跑道”（piste）：与 chemin 同属“道路”语义场，但 piste 更偏向非铺装小路或特定用途路线，是强干扰项。","B":"干扰项 B “广场 / 位置”（place）：与“路径”语义部分重叠，但不符合“去上班所走的路线”这一语境。","D":"干扰项 D “田野 / 场地”（champ）：属于空间概念，但与日常通勤路径无关，用于检测语义区分能力。","C":"此题为对照词。法语 chemin = “道路 / 路径”，英语无直接同源词。"}',
    q.display_condition_json = 'null'
WHERE i.questionnaire_version_id = @version_id AND i.item_code = 'P3-01' AND i.deleted = 0 AND q.deleted = 0;
UPDATE assessment_questionnaire_item
SET required_answer = true, scored = true, weight = 1,
    transfer_category = 'COGNATE',
    context_level = 'CLOZE',
    construct_code = 'CONTEXT_REPAIR',
    target_word = 'chemin',
    option_explanations_json = '{"A":"干扰项 A “小路 / 跑道”（piste）：与 chemin 同属“道路”语义场，但 piste 更偏向非铺装小路或特定用途路线，是强干扰项。","B":"干扰项 B “广场 / 位置”（place）：与“路径”语义部分重叠，但不符合“去上班所走的路线”这一语境。","D":"干扰项 D “田野 / 场地”（champ）：属于空间概念，但与日常通勤路径无关，用于检测语义区分能力。","C":"此题为对照词。法语 chemin = “道路 / 路径”，英语无直接同源词。"}',
    display_condition_json = 'null'
WHERE questionnaire_version_id = @version_id AND item_code = 'P3-01' AND deleted = 0;
UPDATE assessment_question_version
SET question_type = 'SINGLE_CHOICE',
    stem_text = 'Une journée de travail — blanc (1)',
    prompt_text = '请阅读上面的完整短文，为空格 (1) 选择最佳答案。',
    options_json = '[{"key":"A","label":"piste"},{"key":"B","label":"place"},{"key":"C","label":"chemin"},{"key":"D","label":"champ"}]',
    correct_answer_json = '["C"]',
    explanation_text = '1. 答案：C. chemin
#题目分析：答案：C
此题为对照词。法语 chemin = “道路 / 路径”，英语无直接同源词。
干扰项 A “小路 / 跑道”（piste）：与 chemin 同属“道路”语义场，但 piste 更偏向非铺装小路或特定用途路线，是强干扰项。
干扰项 B “广场 / 位置”（place）：与“路径”语义部分重叠，但不符合“去上班所走的路线”这一语境。
干扰项 D “田野 / 场地”（champ）：属于空间概念，但与日常通勤路径无关，用于检测语义区分能力。',
    option_explanations_json = '{"A":"干扰项 A “小路 / 跑道”（piste）：与 chemin 同属“道路”语义场，但 piste 更偏向非铺装小路或特定用途路线，是强干扰项。","B":"干扰项 B “广场 / 位置”（place）：与“路径”语义部分重叠，但不符合“去上班所走的路线”这一语境。","D":"干扰项 D “田野 / 场地”（champ）：属于空间概念，但与日常通勤路径无关，用于检测语义区分能力。","C":"此题为对照词。法语 chemin = “道路 / 路径”，英语无直接同源词。"}',
    required_answer = true, weight = 1,
    transfer_category = 'COGNATE',
    context_level = 'CLOZE',
    construct_code = 'CONTEXT_REPAIR',
    target_word = 'chemin',
    display_condition_json = 'null',
    source_reference = 'questionnaire:P3-01',
    content_hash = '18d984c196e70260add4ba40c192712f507b462ee5c6b9fca8515f3ac22111e6'
WHERE question_bank_id = @bank_id AND question_code = 'P3-01' AND version_no = 1 AND deleted = 0;

-- P3-02
UPDATE assessment_question q
JOIN assessment_questionnaire_item i ON i.assessment_question_id = q.id
SET q.question_type = 'SINGLE_CHOICE',
    q.stem_text = 'Une journée de travail — blanc (2)',
    q.prompt_text = '请阅读上面的完整短文，为空格 (2) 选择最佳答案。',
    q.options_json = '[{"key":"A","label":"garder"},{"key":"B","label":"prendre"},{"key":"C","label":"laisser"},{"key":"D","label":"manquer"}]',
    q.correct_answer_json = '["D"]',
    q.explanation_text = '2. 答案：D. manquer
#题目分析：答案：D
此题为对照词。法语 manquer = “错过”（manquer son train = 错过火车），英语无直接同源词。
干扰项 A “保留”（garder）：与“错过”语义无关，但学生可能因“保留车票”等联想而误选。
干扰项 B “乘坐”（prendre）：prendre le train = “乘火车”，与 manquer le train 语义相反，是强干扰项。
干扰项 C “留下”（laisser）：laisser le train 可能被误解为“让火车离开”，但法语中并不这样搭配，用于检测动宾搭配能力。',
    q.score = 1,
    q.section_code = 'P3',
    q.required_answer = true,
    q.weight = 1,
    q.transfer_category = 'COGNATE',
    q.context_level = 'CLOZE',
    q.construct_code = 'CONTEXT_REPAIR',
    q.target_word = 'manquer',
    q.option_explanations_json = '{"A":"干扰项 A “保留”（garder）：与“错过”语义无关，但学生可能因“保留车票”等联想而误选。","B":"干扰项 B “乘坐”（prendre）：prendre le train = “乘火车”，与 manquer le train 语义相反，是强干扰项。","C":"干扰项 C “留下”（laisser）：laisser le train 可能被误解为“让火车离开”，但法语中并不这样搭配，用于检测动宾搭配能力。","D":"此题为对照词。法语 manquer = “错过”（manquer son train = 错过火车），英语无直接同源词。"}',
    q.display_condition_json = 'null'
WHERE i.questionnaire_version_id = @version_id AND i.item_code = 'P3-02' AND i.deleted = 0 AND q.deleted = 0;
UPDATE assessment_questionnaire_item
SET required_answer = true, scored = true, weight = 1,
    transfer_category = 'COGNATE',
    context_level = 'CLOZE',
    construct_code = 'CONTEXT_REPAIR',
    target_word = 'manquer',
    option_explanations_json = '{"A":"干扰项 A “保留”（garder）：与“错过”语义无关，但学生可能因“保留车票”等联想而误选。","B":"干扰项 B “乘坐”（prendre）：prendre le train = “乘火车”，与 manquer le train 语义相反，是强干扰项。","C":"干扰项 C “留下”（laisser）：laisser le train 可能被误解为“让火车离开”，但法语中并不这样搭配，用于检测动宾搭配能力。","D":"此题为对照词。法语 manquer = “错过”（manquer son train = 错过火车），英语无直接同源词。"}',
    display_condition_json = 'null'
WHERE questionnaire_version_id = @version_id AND item_code = 'P3-02' AND deleted = 0;
UPDATE assessment_question_version
SET question_type = 'SINGLE_CHOICE',
    stem_text = 'Une journée de travail — blanc (2)',
    prompt_text = '请阅读上面的完整短文，为空格 (2) 选择最佳答案。',
    options_json = '[{"key":"A","label":"garder"},{"key":"B","label":"prendre"},{"key":"C","label":"laisser"},{"key":"D","label":"manquer"}]',
    correct_answer_json = '["D"]',
    explanation_text = '2. 答案：D. manquer
#题目分析：答案：D
此题为对照词。法语 manquer = “错过”（manquer son train = 错过火车），英语无直接同源词。
干扰项 A “保留”（garder）：与“错过”语义无关，但学生可能因“保留车票”等联想而误选。
干扰项 B “乘坐”（prendre）：prendre le train = “乘火车”，与 manquer le train 语义相反，是强干扰项。
干扰项 C “留下”（laisser）：laisser le train 可能被误解为“让火车离开”，但法语中并不这样搭配，用于检测动宾搭配能力。',
    option_explanations_json = '{"A":"干扰项 A “保留”（garder）：与“错过”语义无关，但学生可能因“保留车票”等联想而误选。","B":"干扰项 B “乘坐”（prendre）：prendre le train = “乘火车”，与 manquer le train 语义相反，是强干扰项。","C":"干扰项 C “留下”（laisser）：laisser le train 可能被误解为“让火车离开”，但法语中并不这样搭配，用于检测动宾搭配能力。","D":"此题为对照词。法语 manquer = “错过”（manquer son train = 错过火车），英语无直接同源词。"}',
    required_answer = true, weight = 1,
    transfer_category = 'COGNATE',
    context_level = 'CLOZE',
    construct_code = 'CONTEXT_REPAIR',
    target_word = 'manquer',
    display_condition_json = 'null',
    source_reference = 'questionnaire:P3-02',
    content_hash = '45e615247b0159aeb44ed4e43afab86c53f9542dfa297b5de75f1e99a654abc4'
WHERE question_bank_id = @bank_id AND question_code = 'P3-02' AND version_no = 1 AND deleted = 0;

-- P3-03
UPDATE assessment_question q
JOIN assessment_questionnaire_item i ON i.assessment_question_id = q.id
SET q.question_type = 'SINGLE_CHOICE',
    q.stem_text = 'Une journée de travail — blanc (3)',
    q.prompt_text = '请阅读上面的完整短文，为空格 (3) 选择最佳答案。',
    q.options_json = '[{"key":"A","label":"coutume"},{"key":"B","label":"habit"},{"key":"C","label":"geste"},{"key":"D","label":"manière"}]',
    q.correct_answer_json = '["B"]',
    q.explanation_text = '3. 答案：B. habit
#题目分析：答案：B
此题为假朋友（同形异义词）。法语 habit = “服装 / 正装”，英语形似词 habit = “习惯”。
干扰项 A “习惯”（coutume）：直接对应英语 habit 的语义，是负迁移的主要来源。受英语影响的学生可能误以为 habit 意为“习惯”，从而选择 coutume。
干扰项 C “手势”（geste）：与“服装”语义无关，但同属“人的外在表现”范畴，用于检测语义域区分能力。
干扰项 D “方式”（manière）：与“习惯”语义相近，但与“服装”无关。学生若将 habit 误判为“习惯”，可能进一步在 coutume 和 manière 之间犹豫。',
    q.score = 1,
    q.section_code = 'P3',
    q.required_answer = true,
    q.weight = 1,
    q.transfer_category = 'FALSE_FRIEND',
    q.context_level = 'CLOZE',
    q.construct_code = 'CONTEXT_REPAIR',
    q.target_word = 'habit',
    q.option_explanations_json = '{"A":"干扰项 A “习惯”（coutume）：直接对应英语 habit 的语义，是负迁移的主要来源。受英语影响的学生可能误以为 habit 意为“习惯”，从而选择 coutume。","C":"干扰项 C “手势”（geste）：与“服装”语义无关，但同属“人的外在表现”范畴，用于检测语义域区分能力。","D":"干扰项 D “方式”（manière）：与“习惯”语义相近，但与“服装”无关。学生若将 habit 误判为“习惯”，可能进一步在 coutume 和 manière 之间犹豫。","B":"此题为假朋友（同形异义词）。法语 habit = “服装 / 正装”，英语形似词 habit = “习惯”。"}',
    q.display_condition_json = 'null'
WHERE i.questionnaire_version_id = @version_id AND i.item_code = 'P3-03' AND i.deleted = 0 AND q.deleted = 0;
UPDATE assessment_questionnaire_item
SET required_answer = true, scored = true, weight = 1,
    transfer_category = 'FALSE_FRIEND',
    context_level = 'CLOZE',
    construct_code = 'CONTEXT_REPAIR',
    target_word = 'habit',
    option_explanations_json = '{"A":"干扰项 A “习惯”（coutume）：直接对应英语 habit 的语义，是负迁移的主要来源。受英语影响的学生可能误以为 habit 意为“习惯”，从而选择 coutume。","C":"干扰项 C “手势”（geste）：与“服装”语义无关，但同属“人的外在表现”范畴，用于检测语义域区分能力。","D":"干扰项 D “方式”（manière）：与“习惯”语义相近，但与“服装”无关。学生若将 habit 误判为“习惯”，可能进一步在 coutume 和 manière 之间犹豫。","B":"此题为假朋友（同形异义词）。法语 habit = “服装 / 正装”，英语形似词 habit = “习惯”。"}',
    display_condition_json = 'null'
WHERE questionnaire_version_id = @version_id AND item_code = 'P3-03' AND deleted = 0;
UPDATE assessment_question_version
SET question_type = 'SINGLE_CHOICE',
    stem_text = 'Une journée de travail — blanc (3)',
    prompt_text = '请阅读上面的完整短文，为空格 (3) 选择最佳答案。',
    options_json = '[{"key":"A","label":"coutume"},{"key":"B","label":"habit"},{"key":"C","label":"geste"},{"key":"D","label":"manière"}]',
    correct_answer_json = '["B"]',
    explanation_text = '3. 答案：B. habit
#题目分析：答案：B
此题为假朋友（同形异义词）。法语 habit = “服装 / 正装”，英语形似词 habit = “习惯”。
干扰项 A “习惯”（coutume）：直接对应英语 habit 的语义，是负迁移的主要来源。受英语影响的学生可能误以为 habit 意为“习惯”，从而选择 coutume。
干扰项 C “手势”（geste）：与“服装”语义无关，但同属“人的外在表现”范畴，用于检测语义域区分能力。
干扰项 D “方式”（manière）：与“习惯”语义相近，但与“服装”无关。学生若将 habit 误判为“习惯”，可能进一步在 coutume 和 manière 之间犹豫。',
    option_explanations_json = '{"A":"干扰项 A “习惯”（coutume）：直接对应英语 habit 的语义，是负迁移的主要来源。受英语影响的学生可能误以为 habit 意为“习惯”，从而选择 coutume。","C":"干扰项 C “手势”（geste）：与“服装”语义无关，但同属“人的外在表现”范畴，用于检测语义域区分能力。","D":"干扰项 D “方式”（manière）：与“习惯”语义相近，但与“服装”无关。学生若将 habit 误判为“习惯”，可能进一步在 coutume 和 manière 之间犹豫。","B":"此题为假朋友（同形异义词）。法语 habit = “服装 / 正装”，英语形似词 habit = “习惯”。"}',
    required_answer = true, weight = 1,
    transfer_category = 'FALSE_FRIEND',
    context_level = 'CLOZE',
    construct_code = 'CONTEXT_REPAIR',
    target_word = 'habit',
    display_condition_json = 'null',
    source_reference = 'questionnaire:P3-03',
    content_hash = 'b13ee00caa3d2bcfef08d387e8dfad8d5b4ff98a7b2ccc75128cb51c218a9ab5'
WHERE question_bank_id = @bank_id AND question_code = 'P3-03' AND version_no = 1 AND deleted = 0;

-- P3-04
UPDATE assessment_question q
JOIN assessment_questionnaire_item i ON i.assessment_question_id = q.id
SET q.question_type = 'SINGLE_CHOICE',
    q.stem_text = 'Une journée de travail — blanc (4)',
    q.prompt_text = '请阅读上面的完整短文，为空格 (4) 选择最佳答案。',
    q.options_json = '[{"key":"A","label":"emploi"},{"key":"B","label":"stage"},{"key":"C","label":"salaire"},{"key":"D","label":"chômage"}]',
    q.correct_answer_json = '["A"]',
    q.explanation_text = '4. 答案：A. emploi
#题目分析：答案：A
此题为同源词。法语 emploi = “工作 / 职业”，同源英语 employment = “就业 / 职业”。学生在英语正迁移帮助下应能理解此词。
干扰项 B “实习”（stage）：与 emploi 同属“职业活动”语义场，但实习通常是短期、非正式的，学生可能因语义相近而误选。
干扰项 C “工资”（salaire）：与工作相关但非工作本身，学生可能将“工作”与“工资”混淆，属于语义联想过度。
干扰项 D “失业”（chômage）：与 emploi 语义相反。',
    q.score = 1,
    q.section_code = 'P3',
    q.required_answer = true,
    q.weight = 1,
    q.transfer_category = 'COGNATE',
    q.context_level = 'CLOZE',
    q.construct_code = 'CONTEXT_REPAIR',
    q.target_word = 'emploi',
    q.option_explanations_json = '{"B":"干扰项 B “实习”（stage）：与 emploi 同属“职业活动”语义场，但实习通常是短期、非正式的，学生可能因语义相近而误选。","C":"干扰项 C “工资”（salaire）：与工作相关但非工作本身，学生可能将“工作”与“工资”混淆，属于语义联想过度。","D":"干扰项 D “失业”（chômage）：与 emploi 语义相反。","A":"此题为同源词。法语 emploi = “工作 / 职业”，同源英语 employment = “就业 / 职业”。学生在英语正迁移帮助下应能理解此词。"}',
    q.display_condition_json = 'null'
WHERE i.questionnaire_version_id = @version_id AND i.item_code = 'P3-04' AND i.deleted = 0 AND q.deleted = 0;
UPDATE assessment_questionnaire_item
SET required_answer = true, scored = true, weight = 1,
    transfer_category = 'COGNATE',
    context_level = 'CLOZE',
    construct_code = 'CONTEXT_REPAIR',
    target_word = 'emploi',
    option_explanations_json = '{"B":"干扰项 B “实习”（stage）：与 emploi 同属“职业活动”语义场，但实习通常是短期、非正式的，学生可能因语义相近而误选。","C":"干扰项 C “工资”（salaire）：与工作相关但非工作本身，学生可能将“工作”与“工资”混淆，属于语义联想过度。","D":"干扰项 D “失业”（chômage）：与 emploi 语义相反。","A":"此题为同源词。法语 emploi = “工作 / 职业”，同源英语 employment = “就业 / 职业”。学生在英语正迁移帮助下应能理解此词。"}',
    display_condition_json = 'null'
WHERE questionnaire_version_id = @version_id AND item_code = 'P3-04' AND deleted = 0;
UPDATE assessment_question_version
SET question_type = 'SINGLE_CHOICE',
    stem_text = 'Une journée de travail — blanc (4)',
    prompt_text = '请阅读上面的完整短文，为空格 (4) 选择最佳答案。',
    options_json = '[{"key":"A","label":"emploi"},{"key":"B","label":"stage"},{"key":"C","label":"salaire"},{"key":"D","label":"chômage"}]',
    correct_answer_json = '["A"]',
    explanation_text = '4. 答案：A. emploi
#题目分析：答案：A
此题为同源词。法语 emploi = “工作 / 职业”，同源英语 employment = “就业 / 职业”。学生在英语正迁移帮助下应能理解此词。
干扰项 B “实习”（stage）：与 emploi 同属“职业活动”语义场，但实习通常是短期、非正式的，学生可能因语义相近而误选。
干扰项 C “工资”（salaire）：与工作相关但非工作本身，学生可能将“工作”与“工资”混淆，属于语义联想过度。
干扰项 D “失业”（chômage）：与 emploi 语义相反。',
    option_explanations_json = '{"B":"干扰项 B “实习”（stage）：与 emploi 同属“职业活动”语义场，但实习通常是短期、非正式的，学生可能因语义相近而误选。","C":"干扰项 C “工资”（salaire）：与工作相关但非工作本身，学生可能将“工作”与“工资”混淆，属于语义联想过度。","D":"干扰项 D “失业”（chômage）：与 emploi 语义相反。","A":"此题为同源词。法语 emploi = “工作 / 职业”，同源英语 employment = “就业 / 职业”。学生在英语正迁移帮助下应能理解此词。"}',
    required_answer = true, weight = 1,
    transfer_category = 'COGNATE',
    context_level = 'CLOZE',
    construct_code = 'CONTEXT_REPAIR',
    target_word = 'emploi',
    display_condition_json = 'null',
    source_reference = 'questionnaire:P3-04',
    content_hash = '61fd221117f2a6a040e3ad9403741a327d43af6ed44036b38cd683d8cc54ddcf'
WHERE question_bank_id = @bank_id AND question_code = 'P3-04' AND version_no = 1 AND deleted = 0;

-- P3-05
UPDATE assessment_question q
JOIN assessment_questionnaire_item i ON i.assessment_question_id = q.id
SET q.question_type = 'SINGLE_CHOICE',
    q.stem_text = 'Une journée de travail — blanc (5)',
    q.prompt_text = '请阅读上面的完整短文，为空格 (5) 选择最佳答案。',
    q.options_json = '[{"key":"A","label":"réfléchir"},{"key":"B","label":"résoudre"},{"key":"C","label":"rejoindre"},{"key":"D","label":"répondre"}]',
    q.correct_answer_json = '["D"]',
    q.explanation_text = '5. 答案：D. répondre
#题目分析：答案：D
此题为同源词。法语 répondre = “回答 / 回复”（répondre aux courriels = 回复邮件），同源英语 respond = “回应”。
干扰项 A “思考”（réfléchir）：与“回复邮件”语义相关（回复前需要思考），但 réfléchir 不直接接 à 表示“回复”，学生可能因语义联想而误选。
干扰项 B “解决”（résoudre）：résoudre un problème = “解决问题”，但 résoudre aux courriels 不成立，学生可能将“处理邮件”与“解决邮件”混淆。
干扰项 C “加入”（rejoindre）：rejoindre quelqu’un = “与某人会合”，但 rejoindre aux courriels 不成立，用于检测动词与介词搭配能力。',
    q.score = 1,
    q.section_code = 'P3',
    q.required_answer = true,
    q.weight = 1,
    q.transfer_category = 'COGNATE',
    q.context_level = 'CLOZE',
    q.construct_code = 'CONTEXT_REPAIR',
    q.target_word = 'répondre',
    q.option_explanations_json = '{"A":"干扰项 A “思考”（réfléchir）：与“回复邮件”语义相关（回复前需要思考），但 réfléchir 不直接接 à 表示“回复”，学生可能因语义联想而误选。","B":"干扰项 B “解决”（résoudre）：résoudre un problème = “解决问题”，但 résoudre aux courriels 不成立，学生可能将“处理邮件”与“解决邮件”混淆。","C":"干扰项 C “加入”（rejoindre）：rejoindre quelqu’un = “与某人会合”，但 rejoindre aux courriels 不成立，用于检测动词与介词搭配能力。","D":"此题为同源词。法语 répondre = “回答 / 回复”（répondre aux courriels = 回复邮件），同源英语 respond = “回应”。"}',
    q.display_condition_json = 'null'
WHERE i.questionnaire_version_id = @version_id AND i.item_code = 'P3-05' AND i.deleted = 0 AND q.deleted = 0;
UPDATE assessment_questionnaire_item
SET required_answer = true, scored = true, weight = 1,
    transfer_category = 'COGNATE',
    context_level = 'CLOZE',
    construct_code = 'CONTEXT_REPAIR',
    target_word = 'répondre',
    option_explanations_json = '{"A":"干扰项 A “思考”（réfléchir）：与“回复邮件”语义相关（回复前需要思考），但 réfléchir 不直接接 à 表示“回复”，学生可能因语义联想而误选。","B":"干扰项 B “解决”（résoudre）：résoudre un problème = “解决问题”，但 résoudre aux courriels 不成立，学生可能将“处理邮件”与“解决邮件”混淆。","C":"干扰项 C “加入”（rejoindre）：rejoindre quelqu’un = “与某人会合”，但 rejoindre aux courriels 不成立，用于检测动词与介词搭配能力。","D":"此题为同源词。法语 répondre = “回答 / 回复”（répondre aux courriels = 回复邮件），同源英语 respond = “回应”。"}',
    display_condition_json = 'null'
WHERE questionnaire_version_id = @version_id AND item_code = 'P3-05' AND deleted = 0;
UPDATE assessment_question_version
SET question_type = 'SINGLE_CHOICE',
    stem_text = 'Une journée de travail — blanc (5)',
    prompt_text = '请阅读上面的完整短文，为空格 (5) 选择最佳答案。',
    options_json = '[{"key":"A","label":"réfléchir"},{"key":"B","label":"résoudre"},{"key":"C","label":"rejoindre"},{"key":"D","label":"répondre"}]',
    correct_answer_json = '["D"]',
    explanation_text = '5. 答案：D. répondre
#题目分析：答案：D
此题为同源词。法语 répondre = “回答 / 回复”（répondre aux courriels = 回复邮件），同源英语 respond = “回应”。
干扰项 A “思考”（réfléchir）：与“回复邮件”语义相关（回复前需要思考），但 réfléchir 不直接接 à 表示“回复”，学生可能因语义联想而误选。
干扰项 B “解决”（résoudre）：résoudre un problème = “解决问题”，但 résoudre aux courriels 不成立，学生可能将“处理邮件”与“解决邮件”混淆。
干扰项 C “加入”（rejoindre）：rejoindre quelqu’un = “与某人会合”，但 rejoindre aux courriels 不成立，用于检测动词与介词搭配能力。',
    option_explanations_json = '{"A":"干扰项 A “思考”（réfléchir）：与“回复邮件”语义相关（回复前需要思考），但 réfléchir 不直接接 à 表示“回复”，学生可能因语义联想而误选。","B":"干扰项 B “解决”（résoudre）：résoudre un problème = “解决问题”，但 résoudre aux courriels 不成立，学生可能将“处理邮件”与“解决邮件”混淆。","C":"干扰项 C “加入”（rejoindre）：rejoindre quelqu’un = “与某人会合”，但 rejoindre aux courriels 不成立，用于检测动词与介词搭配能力。","D":"此题为同源词。法语 répondre = “回答 / 回复”（répondre aux courriels = 回复邮件），同源英语 respond = “回应”。"}',
    required_answer = true, weight = 1,
    transfer_category = 'COGNATE',
    context_level = 'CLOZE',
    construct_code = 'CONTEXT_REPAIR',
    target_word = 'répondre',
    display_condition_json = 'null',
    source_reference = 'questionnaire:P3-05',
    content_hash = '1206ecee4d4bf1724c3378319515acd409405e0b2d7b7ed1feb9dd40f216126d'
WHERE question_bank_id = @bank_id AND question_code = 'P3-05' AND version_no = 1 AND deleted = 0;

-- P3-06
UPDATE assessment_question q
JOIN assessment_questionnaire_item i ON i.assessment_question_id = q.id
SET q.question_type = 'SINGLE_CHOICE',
    q.stem_text = 'Une journée de travail — blanc (6)',
    q.prompt_text = '请阅读上面的完整短文，为空格 (6) 选择最佳答案。',
    q.options_json = '[{"key":"A","label":"tendance"},{"key":"B","label":"entente"},{"key":"C","label":"attente"},{"key":"D","label":"tente"}]',
    q.correct_answer_json = '["B"]',
    q.explanation_text = '答案：B. entente
#题目分析：
此题为纯法语对照词。法语 entente = "融洽、默契"，指人与人之间良好的理解与合作关系。英语虽有 entente 一词，但属于罕见的外交术语（如 Entente cordiale 友好协约）。
干扰项 A "趋势"（tendance）：与 entente 同以 ten- 开头，且同为 -ance / -ence 结尾，拼写高度相似。但 tendance 指"倾向、趋势"，与"人际关系"完全无关，且 bonne tendance 在此语境中搭配不成立。
干扰项 C "等待"（attente）：与 entente 拼写高度相似（仅首字母 a 与 e 之差），但词义完全不同。attente 指"等待、期望"，与同事关系无任何逻辑关联，且 bonne attente 搭配不成立。
干扰项 D "帐篷"（tente）：与 entente 同为 -ente 结尾，但词义毫不相关。avoir une bonne tente avec ses collègues 在语法和语义上完全不成立，学生若仅凭词形猜测极易误选。',
    q.score = 1,
    q.section_code = 'P3',
    q.required_answer = true,
    q.weight = 1,
    q.transfer_category = 'FRENCH_CONTROL',
    q.context_level = 'CLOZE',
    q.construct_code = 'CONTEXT_REPAIR',
    q.target_word = 'entente',
    q.option_explanations_json = '{"A":"干扰项 A \\"趋势\\"（tendance）：与 entente 同以 ten- 开头，且同为 -ance / -ence 结尾，拼写高度相似。但 tendance 指\\"倾向、趋势\\"，与\\"人际关系\\"完全无关，且 bonne tendance 在此语境中搭配不成立。","C":"干扰项 C \\"等待\\"（attente）：与 entente 拼写高度相似（仅首字母 a 与 e 之差），但词义完全不同。attente 指\\"等待、期望\\"，与同事关系无任何逻辑关联，且 bonne attente 搭配不成立。","D":"干扰项 D \\"帐篷\\"（tente）：与 entente 同为 -ente 结尾，但词义毫不相关。avoir une bonne tente avec ses collègues 在语法和语义上完全不成立，学生若仅凭词形猜测极易误选。","B":"#题目分析：\\n此题为纯法语对照词。法语 entente = \\"融洽、默契\\"，指人与人之间良好的理解与合作关系。英语虽有 entente 一词，但属于罕见的外交术语（如 Entente cordiale 友好协约）。"}',
    q.display_condition_json = 'null'
WHERE i.questionnaire_version_id = @version_id AND i.item_code = 'P3-06' AND i.deleted = 0 AND q.deleted = 0;
UPDATE assessment_questionnaire_item
SET required_answer = true, scored = true, weight = 1,
    transfer_category = 'FRENCH_CONTROL',
    context_level = 'CLOZE',
    construct_code = 'CONTEXT_REPAIR',
    target_word = 'entente',
    option_explanations_json = '{"A":"干扰项 A \\"趋势\\"（tendance）：与 entente 同以 ten- 开头，且同为 -ance / -ence 结尾，拼写高度相似。但 tendance 指\\"倾向、趋势\\"，与\\"人际关系\\"完全无关，且 bonne tendance 在此语境中搭配不成立。","C":"干扰项 C \\"等待\\"（attente）：与 entente 拼写高度相似（仅首字母 a 与 e 之差），但词义完全不同。attente 指\\"等待、期望\\"，与同事关系无任何逻辑关联，且 bonne attente 搭配不成立。","D":"干扰项 D \\"帐篷\\"（tente）：与 entente 同为 -ente 结尾，但词义毫不相关。avoir une bonne tente avec ses collègues 在语法和语义上完全不成立，学生若仅凭词形猜测极易误选。","B":"#题目分析：\\n此题为纯法语对照词。法语 entente = \\"融洽、默契\\"，指人与人之间良好的理解与合作关系。英语虽有 entente 一词，但属于罕见的外交术语（如 Entente cordiale 友好协约）。"}',
    display_condition_json = 'null'
WHERE questionnaire_version_id = @version_id AND item_code = 'P3-06' AND deleted = 0;
UPDATE assessment_question_version
SET question_type = 'SINGLE_CHOICE',
    stem_text = 'Une journée de travail — blanc (6)',
    prompt_text = '请阅读上面的完整短文，为空格 (6) 选择最佳答案。',
    options_json = '[{"key":"A","label":"tendance"},{"key":"B","label":"entente"},{"key":"C","label":"attente"},{"key":"D","label":"tente"}]',
    correct_answer_json = '["B"]',
    explanation_text = '答案：B. entente
#题目分析：
此题为纯法语对照词。法语 entente = "融洽、默契"，指人与人之间良好的理解与合作关系。英语虽有 entente 一词，但属于罕见的外交术语（如 Entente cordiale 友好协约）。
干扰项 A "趋势"（tendance）：与 entente 同以 ten- 开头，且同为 -ance / -ence 结尾，拼写高度相似。但 tendance 指"倾向、趋势"，与"人际关系"完全无关，且 bonne tendance 在此语境中搭配不成立。
干扰项 C "等待"（attente）：与 entente 拼写高度相似（仅首字母 a 与 e 之差），但词义完全不同。attente 指"等待、期望"，与同事关系无任何逻辑关联，且 bonne attente 搭配不成立。
干扰项 D "帐篷"（tente）：与 entente 同为 -ente 结尾，但词义毫不相关。avoir une bonne tente avec ses collègues 在语法和语义上完全不成立，学生若仅凭词形猜测极易误选。',
    option_explanations_json = '{"A":"干扰项 A \\"趋势\\"（tendance）：与 entente 同以 ten- 开头，且同为 -ance / -ence 结尾，拼写高度相似。但 tendance 指\\"倾向、趋势\\"，与\\"人际关系\\"完全无关，且 bonne tendance 在此语境中搭配不成立。","C":"干扰项 C \\"等待\\"（attente）：与 entente 拼写高度相似（仅首字母 a 与 e 之差），但词义完全不同。attente 指\\"等待、期望\\"，与同事关系无任何逻辑关联，且 bonne attente 搭配不成立。","D":"干扰项 D \\"帐篷\\"（tente）：与 entente 同为 -ente 结尾，但词义毫不相关。avoir une bonne tente avec ses collègues 在语法和语义上完全不成立，学生若仅凭词形猜测极易误选。","B":"#题目分析：\\n此题为纯法语对照词。法语 entente = \\"融洽、默契\\"，指人与人之间良好的理解与合作关系。英语虽有 entente 一词，但属于罕见的外交术语（如 Entente cordiale 友好协约）。"}',
    required_answer = true, weight = 1,
    transfer_category = 'FRENCH_CONTROL',
    context_level = 'CLOZE',
    construct_code = 'CONTEXT_REPAIR',
    target_word = 'entente',
    display_condition_json = 'null',
    source_reference = 'questionnaire:P3-06',
    content_hash = '90a6cfcd328e464402e0c4bc8ed54a8b7a8b17b1bbf233f4b968b5bc86d588e8'
WHERE question_bank_id = @bank_id AND question_code = 'P3-06' AND version_no = 1 AND deleted = 0;

-- P3-07
UPDATE assessment_question q
JOIN assessment_questionnaire_item i ON i.assessment_question_id = q.id
SET q.question_type = 'SINGLE_CHOICE',
    q.stem_text = 'Une journée de travail — blanc (7)',
    q.prompt_text = '请阅读上面的完整短文，为空格 (7) 选择最佳答案。',
    q.options_json = '[{"key":"A","label":"soutenir"},{"key":"B","label":"accepter"},{"key":"C","label":"assister"},{"key":"D","label":"attaquer"}]',
    q.correct_answer_json = '["C"]',
    q.explanation_text = '7. 答案：C. assister
#题目分析：答案：C
此题为假朋友（同形异义词）。法语 assister à = “参加 / 出席”，英语形似词 assist = “帮助 / 协助”。
干扰项 A “支持 / 帮助”（soutenir）：直接对应英语 assist 的语义，是负迁移的主要来源。受英语影响的学生可能误以为 assister 意为“帮助”，从而选择 soutenir。
干扰项 B “接受”（accepter）：与“参加会议”语义相关（接受邀请才能参加会议），但 accepter à une réunion 不成立，用于检测动词搭配能力。
干扰项 D “攻击”（attaquer）：语义上与“参加会议”无关，与 soutenir（支持）形成反义对比，用于检测极端错误选择。',
    q.score = 1,
    q.section_code = 'P3',
    q.required_answer = true,
    q.weight = 1,
    q.transfer_category = 'FALSE_FRIEND',
    q.context_level = 'CLOZE',
    q.construct_code = 'CONTEXT_REPAIR',
    q.target_word = 'assister',
    q.option_explanations_json = '{"A":"干扰项 A “支持 / 帮助”（soutenir）：直接对应英语 assist 的语义，是负迁移的主要来源。受英语影响的学生可能误以为 assister 意为“帮助”，从而选择 soutenir。","B":"干扰项 B “接受”（accepter）：与“参加会议”语义相关（接受邀请才能参加会议），但 accepter à une réunion 不成立，用于检测动词搭配能力。","D":"干扰项 D “攻击”（attaquer）：语义上与“参加会议”无关，与 soutenir（支持）形成反义对比，用于检测极端错误选择。","C":"此题为假朋友（同形异义词）。法语 assister à = “参加 / 出席”，英语形似词 assist = “帮助 / 协助”。"}',
    q.display_condition_json = 'null'
WHERE i.questionnaire_version_id = @version_id AND i.item_code = 'P3-07' AND i.deleted = 0 AND q.deleted = 0;
UPDATE assessment_questionnaire_item
SET required_answer = true, scored = true, weight = 1,
    transfer_category = 'FALSE_FRIEND',
    context_level = 'CLOZE',
    construct_code = 'CONTEXT_REPAIR',
    target_word = 'assister',
    option_explanations_json = '{"A":"干扰项 A “支持 / 帮助”（soutenir）：直接对应英语 assist 的语义，是负迁移的主要来源。受英语影响的学生可能误以为 assister 意为“帮助”，从而选择 soutenir。","B":"干扰项 B “接受”（accepter）：与“参加会议”语义相关（接受邀请才能参加会议），但 accepter à une réunion 不成立，用于检测动词搭配能力。","D":"干扰项 D “攻击”（attaquer）：语义上与“参加会议”无关，与 soutenir（支持）形成反义对比，用于检测极端错误选择。","C":"此题为假朋友（同形异义词）。法语 assister à = “参加 / 出席”，英语形似词 assist = “帮助 / 协助”。"}',
    display_condition_json = 'null'
WHERE questionnaire_version_id = @version_id AND item_code = 'P3-07' AND deleted = 0;
UPDATE assessment_question_version
SET question_type = 'SINGLE_CHOICE',
    stem_text = 'Une journée de travail — blanc (7)',
    prompt_text = '请阅读上面的完整短文，为空格 (7) 选择最佳答案。',
    options_json = '[{"key":"A","label":"soutenir"},{"key":"B","label":"accepter"},{"key":"C","label":"assister"},{"key":"D","label":"attaquer"}]',
    correct_answer_json = '["C"]',
    explanation_text = '7. 答案：C. assister
#题目分析：答案：C
此题为假朋友（同形异义词）。法语 assister à = “参加 / 出席”，英语形似词 assist = “帮助 / 协助”。
干扰项 A “支持 / 帮助”（soutenir）：直接对应英语 assist 的语义，是负迁移的主要来源。受英语影响的学生可能误以为 assister 意为“帮助”，从而选择 soutenir。
干扰项 B “接受”（accepter）：与“参加会议”语义相关（接受邀请才能参加会议），但 accepter à une réunion 不成立，用于检测动词搭配能力。
干扰项 D “攻击”（attaquer）：语义上与“参加会议”无关，与 soutenir（支持）形成反义对比，用于检测极端错误选择。',
    option_explanations_json = '{"A":"干扰项 A “支持 / 帮助”（soutenir）：直接对应英语 assist 的语义，是负迁移的主要来源。受英语影响的学生可能误以为 assister 意为“帮助”，从而选择 soutenir。","B":"干扰项 B “接受”（accepter）：与“参加会议”语义相关（接受邀请才能参加会议），但 accepter à une réunion 不成立，用于检测动词搭配能力。","D":"干扰项 D “攻击”（attaquer）：语义上与“参加会议”无关，与 soutenir（支持）形成反义对比，用于检测极端错误选择。","C":"此题为假朋友（同形异义词）。法语 assister à = “参加 / 出席”，英语形似词 assist = “帮助 / 协助”。"}',
    required_answer = true, weight = 1,
    transfer_category = 'FALSE_FRIEND',
    context_level = 'CLOZE',
    construct_code = 'CONTEXT_REPAIR',
    target_word = 'assister',
    display_condition_json = 'null',
    source_reference = 'questionnaire:P3-07',
    content_hash = 'ca2200c8a6becade8f72e45761012b2b8757c626f3686e1f45a52c5a7124997a'
WHERE question_bank_id = @bank_id AND question_code = 'P3-07' AND version_no = 1 AND deleted = 0;

-- P3-08
UPDATE assessment_question q
JOIN assessment_questionnaire_item i ON i.assessment_question_id = q.id
SET q.question_type = 'SINGLE_CHOICE',
    q.stem_text = 'Une journée de travail — blanc (8)',
    q.prompt_text = '请阅读上面的完整短文，为空格 (8) 选择最佳答案。',
    q.options_json = '[{"key":"A","label":"décevoir"},{"key":"B","label":"tromper"},{"key":"C","label":"menacer"},{"key":"D","label":"blesser"}]',
    q.correct_answer_json = '["A"]',
    q.explanation_text = '8. 答案：A. décevoir
#题目分析：答案：A
此题为假朋友（同形异义词）。法语 décevoir = “使失望”，英语形似词 deceive = “欺骗”。
干扰项 B “欺骗”（tromper）：直接对应英语 deceive 的语义，是负迁移的主要来源。受英语影响的学生可能误以为 décevoir 意为“欺骗”，从而选择 tromper。
干扰项 C “威胁”（menacer）：语义上与“失望”无关，但与 décevoir 同属负面行为范畴，用于检测语义域区分能力。
干扰项 D “伤害”（blesser）：语义上与“失望”部分重叠（言语伤害可能导致失望），但 blesser 程度更重、更具体，是强干扰项。',
    q.score = 1,
    q.section_code = 'P3',
    q.required_answer = true,
    q.weight = 1,
    q.transfer_category = 'FALSE_FRIEND',
    q.context_level = 'CLOZE',
    q.construct_code = 'CONTEXT_REPAIR',
    q.target_word = 'décevoir',
    q.option_explanations_json = '{"B":"干扰项 B “欺骗”（tromper）：直接对应英语 deceive 的语义，是负迁移的主要来源。受英语影响的学生可能误以为 décevoir 意为“欺骗”，从而选择 tromper。","C":"干扰项 C “威胁”（menacer）：语义上与“失望”无关，但与 décevoir 同属负面行为范畴，用于检测语义域区分能力。","D":"干扰项 D “伤害”（blesser）：语义上与“失望”部分重叠（言语伤害可能导致失望），但 blesser 程度更重、更具体，是强干扰项。","A":"此题为假朋友（同形异义词）。法语 décevoir = “使失望”，英语形似词 deceive = “欺骗”。"}',
    q.display_condition_json = 'null'
WHERE i.questionnaire_version_id = @version_id AND i.item_code = 'P3-08' AND i.deleted = 0 AND q.deleted = 0;
UPDATE assessment_questionnaire_item
SET required_answer = true, scored = true, weight = 1,
    transfer_category = 'FALSE_FRIEND',
    context_level = 'CLOZE',
    construct_code = 'CONTEXT_REPAIR',
    target_word = 'décevoir',
    option_explanations_json = '{"B":"干扰项 B “欺骗”（tromper）：直接对应英语 deceive 的语义，是负迁移的主要来源。受英语影响的学生可能误以为 décevoir 意为“欺骗”，从而选择 tromper。","C":"干扰项 C “威胁”（menacer）：语义上与“失望”无关，但与 décevoir 同属负面行为范畴，用于检测语义域区分能力。","D":"干扰项 D “伤害”（blesser）：语义上与“失望”部分重叠（言语伤害可能导致失望），但 blesser 程度更重、更具体，是强干扰项。","A":"此题为假朋友（同形异义词）。法语 décevoir = “使失望”，英语形似词 deceive = “欺骗”。"}',
    display_condition_json = 'null'
WHERE questionnaire_version_id = @version_id AND item_code = 'P3-08' AND deleted = 0;
UPDATE assessment_question_version
SET question_type = 'SINGLE_CHOICE',
    stem_text = 'Une journée de travail — blanc (8)',
    prompt_text = '请阅读上面的完整短文，为空格 (8) 选择最佳答案。',
    options_json = '[{"key":"A","label":"décevoir"},{"key":"B","label":"tromper"},{"key":"C","label":"menacer"},{"key":"D","label":"blesser"}]',
    correct_answer_json = '["A"]',
    explanation_text = '8. 答案：A. décevoir
#题目分析：答案：A
此题为假朋友（同形异义词）。法语 décevoir = “使失望”，英语形似词 deceive = “欺骗”。
干扰项 B “欺骗”（tromper）：直接对应英语 deceive 的语义，是负迁移的主要来源。受英语影响的学生可能误以为 décevoir 意为“欺骗”，从而选择 tromper。
干扰项 C “威胁”（menacer）：语义上与“失望”无关，但与 décevoir 同属负面行为范畴，用于检测语义域区分能力。
干扰项 D “伤害”（blesser）：语义上与“失望”部分重叠（言语伤害可能导致失望），但 blesser 程度更重、更具体，是强干扰项。',
    option_explanations_json = '{"B":"干扰项 B “欺骗”（tromper）：直接对应英语 deceive 的语义，是负迁移的主要来源。受英语影响的学生可能误以为 décevoir 意为“欺骗”，从而选择 tromper。","C":"干扰项 C “威胁”（menacer）：语义上与“失望”无关，但与 décevoir 同属负面行为范畴，用于检测语义域区分能力。","D":"干扰项 D “伤害”（blesser）：语义上与“失望”部分重叠（言语伤害可能导致失望），但 blesser 程度更重、更具体，是强干扰项。","A":"此题为假朋友（同形异义词）。法语 décevoir = “使失望”，英语形似词 deceive = “欺骗”。"}',
    required_answer = true, weight = 1,
    transfer_category = 'FALSE_FRIEND',
    context_level = 'CLOZE',
    construct_code = 'CONTEXT_REPAIR',
    target_word = 'décevoir',
    display_condition_json = 'null',
    source_reference = 'questionnaire:P3-08',
    content_hash = '4c0df5fbd20550632cd9b8c42771438800f8b60407c6e943952e5f7db6b02491'
WHERE question_bank_id = @bank_id AND question_code = 'P3-08' AND version_no = 1 AND deleted = 0;

-- P3-09
UPDATE assessment_question q
JOIN assessment_questionnaire_item i ON i.assessment_question_id = q.id
SET q.question_type = 'SINGLE_CHOICE',
    q.stem_text = 'Une journée de travail — blanc (9)',
    q.prompt_text = '请阅读上面的完整短文，为空格 (9) 选择最佳答案。',
    q.options_json = '[{"key":"A","label":"preuve"},{"key":"B","label":"détail"},{"key":"C","label":"évidence"},{"key":"D","label":"chiffre"}]',
    q.correct_answer_json = '["C"]',
    q.explanation_text = '9. 答案：C. évidence
#题目分析：答案：C
此题为假朋友（同形异义词）。法语 évidence = “显而易见的事 / 明摆着的事”（c’est une évidence que... = “很明显……”），英语形似词 evidence = “证据”。
干扰项 A “证据”（preuve）：直接对应英语 evidence 的语义，是负迁移的主要来源。受英语影响的学生可能误以为 évidence 意为“证据”，从而选择 preuve。
干扰项 B “细节”（détail）：与“显而易见”语义部分相关（细节可能是明显的），但 détail 侧重“具体细小的点”，与 évidence 的“整体明显”不同。
干扰项 D “数字”（chiffre）：与“证据 / 明显”语义无关，但 chiffre 常作为 preuve 的一种形式（数字证据），用于检测语义联想过度。',
    q.score = 1,
    q.section_code = 'P3',
    q.required_answer = true,
    q.weight = 1,
    q.transfer_category = 'FALSE_FRIEND',
    q.context_level = 'CLOZE',
    q.construct_code = 'CONTEXT_REPAIR',
    q.target_word = 'évidence',
    q.option_explanations_json = '{"A":"干扰项 A “证据”（preuve）：直接对应英语 evidence 的语义，是负迁移的主要来源。受英语影响的学生可能误以为 évidence 意为“证据”，从而选择 preuve。","B":"干扰项 B “细节”（détail）：与“显而易见”语义部分相关（细节可能是明显的），但 détail 侧重“具体细小的点”，与 évidence 的“整体明显”不同。","D":"干扰项 D “数字”（chiffre）：与“证据 / 明显”语义无关，但 chiffre 常作为 preuve 的一种形式（数字证据），用于检测语义联想过度。","C":"此题为假朋友（同形异义词）。法语 évidence = “显而易见的事 / 明摆着的事”（c’est une évidence que... = “很明显……”），英语形似词 evidence = “证据”。"}',
    q.display_condition_json = 'null'
WHERE i.questionnaire_version_id = @version_id AND i.item_code = 'P3-09' AND i.deleted = 0 AND q.deleted = 0;
UPDATE assessment_questionnaire_item
SET required_answer = true, scored = true, weight = 1,
    transfer_category = 'FALSE_FRIEND',
    context_level = 'CLOZE',
    construct_code = 'CONTEXT_REPAIR',
    target_word = 'évidence',
    option_explanations_json = '{"A":"干扰项 A “证据”（preuve）：直接对应英语 evidence 的语义，是负迁移的主要来源。受英语影响的学生可能误以为 évidence 意为“证据”，从而选择 preuve。","B":"干扰项 B “细节”（détail）：与“显而易见”语义部分相关（细节可能是明显的），但 détail 侧重“具体细小的点”，与 évidence 的“整体明显”不同。","D":"干扰项 D “数字”（chiffre）：与“证据 / 明显”语义无关，但 chiffre 常作为 preuve 的一种形式（数字证据），用于检测语义联想过度。","C":"此题为假朋友（同形异义词）。法语 évidence = “显而易见的事 / 明摆着的事”（c’est une évidence que... = “很明显……”），英语形似词 evidence = “证据”。"}',
    display_condition_json = 'null'
WHERE questionnaire_version_id = @version_id AND item_code = 'P3-09' AND deleted = 0;
UPDATE assessment_question_version
SET question_type = 'SINGLE_CHOICE',
    stem_text = 'Une journée de travail — blanc (9)',
    prompt_text = '请阅读上面的完整短文，为空格 (9) 选择最佳答案。',
    options_json = '[{"key":"A","label":"preuve"},{"key":"B","label":"détail"},{"key":"C","label":"évidence"},{"key":"D","label":"chiffre"}]',
    correct_answer_json = '["C"]',
    explanation_text = '9. 答案：C. évidence
#题目分析：答案：C
此题为假朋友（同形异义词）。法语 évidence = “显而易见的事 / 明摆着的事”（c’est une évidence que... = “很明显……”），英语形似词 evidence = “证据”。
干扰项 A “证据”（preuve）：直接对应英语 evidence 的语义，是负迁移的主要来源。受英语影响的学生可能误以为 évidence 意为“证据”，从而选择 preuve。
干扰项 B “细节”（détail）：与“显而易见”语义部分相关（细节可能是明显的），但 détail 侧重“具体细小的点”，与 évidence 的“整体明显”不同。
干扰项 D “数字”（chiffre）：与“证据 / 明显”语义无关，但 chiffre 常作为 preuve 的一种形式（数字证据），用于检测语义联想过度。',
    option_explanations_json = '{"A":"干扰项 A “证据”（preuve）：直接对应英语 evidence 的语义，是负迁移的主要来源。受英语影响的学生可能误以为 évidence 意为“证据”，从而选择 preuve。","B":"干扰项 B “细节”（détail）：与“显而易见”语义部分相关（细节可能是明显的），但 détail 侧重“具体细小的点”，与 évidence 的“整体明显”不同。","D":"干扰项 D “数字”（chiffre）：与“证据 / 明显”语义无关，但 chiffre 常作为 preuve 的一种形式（数字证据），用于检测语义联想过度。","C":"此题为假朋友（同形异义词）。法语 évidence = “显而易见的事 / 明摆着的事”（c’est une évidence que... = “很明显……”），英语形似词 evidence = “证据”。"}',
    required_answer = true, weight = 1,
    transfer_category = 'FALSE_FRIEND',
    context_level = 'CLOZE',
    construct_code = 'CONTEXT_REPAIR',
    target_word = 'évidence',
    display_condition_json = 'null',
    source_reference = 'questionnaire:P3-09',
    content_hash = 'a9a8ea209f7952b450087ee9da9dab5a33d42a9a2e57d158b21d3b49246f0500'
WHERE question_bank_id = @bank_id AND question_code = 'P3-09' AND version_no = 1 AND deleted = 0;

-- P3-10
UPDATE assessment_question q
JOIN assessment_questionnaire_item i ON i.assessment_question_id = q.id
SET q.question_type = 'SINGLE_CHOICE',
    q.stem_text = 'Une journée de travail — blanc (10)',
    q.prompt_text = '请阅读上面的完整短文，为空格 (10) 选择最佳答案。',
    q.options_json = '[{"key":"A","label":"simple"},{"key":"B","label":"habituelle"},{"key":"C","label":"singulière"},{"key":"D","label":"prudente"}]',
    q.correct_answer_json = '["B"]',
    q.explanation_text = '10. 答案：B. habituel
#题目分析：
此题为英法同源词。法语 habituel = “习惯性的、通常的/常见的”，与英语 habitual 同源且语义基本重合，学生可借助英语正迁移理解词义。
干扰项 A “简单的”（simple）：语义上与“职场挫折”无直接关联，且与后文“明天会好转”缺乏逻辑呼应。学生可能因“简单的事容易过去”这一日常联想而误选，属于语义泛化干扰。
干扰项 C “独特的、奇特的”（singulière）：与上文 tout le monde peut vivre ce genre de déception（每个人都会经历这种失望）逻辑相反——人人都能经历的事，不可能是“独特的”，可快速排除。
干扰项 D “谨慎的”（prudente）：语义方向完全偏离，与“失望/挫折”的情感色彩不匹配，且修饰 situation 不自然，属于随机猜测干扰项。
（1 3 4题为第一组2 5 7题为第二组6 9 10题为第三组8题单独列出）',
    q.score = 1,
    q.section_code = 'P3',
    q.required_answer = true,
    q.weight = 1,
    q.transfer_category = 'COGNATE',
    q.context_level = 'CLOZE',
    q.construct_code = 'CONTEXT_REPAIR',
    q.target_word = 'habituel',
    q.option_explanations_json = '{"A":"干扰项 A “简单的”（simple）：语义上与“职场挫折”无直接关联，且与后文“明天会好转”缺乏逻辑呼应。学生可能因“简单的事容易过去”这一日常联想而误选，属于语义泛化干扰。","C":"干扰项 C “独特的、奇特的”（singulière）：与上文 tout le monde peut vivre ce genre de déception（每个人都会经历这种失望）逻辑相反——人人都能经历的事，不可能是“独特的”，可快速排除。","D":"干扰项 D “谨慎的”（prudente）：语义方向完全偏离，与“失望/挫折”的情感色彩不匹配，且修饰 situation 不自然，属于随机猜测干扰项。","B":"#题目分析：\\n此题为英法同源词。法语 habituel = “习惯性的、通常的/常见的”，与英语 habitual 同源且语义基本重合，学生可借助英语正迁移理解词义。\\n（1 3 4题为第一组2 5 7题为第二组6 9 10题为第三组8题单独列出）"}',
    q.display_condition_json = 'null'
WHERE i.questionnaire_version_id = @version_id AND i.item_code = 'P3-10' AND i.deleted = 0 AND q.deleted = 0;
UPDATE assessment_questionnaire_item
SET required_answer = true, scored = true, weight = 1,
    transfer_category = 'COGNATE',
    context_level = 'CLOZE',
    construct_code = 'CONTEXT_REPAIR',
    target_word = 'habituel',
    option_explanations_json = '{"A":"干扰项 A “简单的”（simple）：语义上与“职场挫折”无直接关联，且与后文“明天会好转”缺乏逻辑呼应。学生可能因“简单的事容易过去”这一日常联想而误选，属于语义泛化干扰。","C":"干扰项 C “独特的、奇特的”（singulière）：与上文 tout le monde peut vivre ce genre de déception（每个人都会经历这种失望）逻辑相反——人人都能经历的事，不可能是“独特的”，可快速排除。","D":"干扰项 D “谨慎的”（prudente）：语义方向完全偏离，与“失望/挫折”的情感色彩不匹配，且修饰 situation 不自然，属于随机猜测干扰项。","B":"#题目分析：\\n此题为英法同源词。法语 habituel = “习惯性的、通常的/常见的”，与英语 habitual 同源且语义基本重合，学生可借助英语正迁移理解词义。\\n（1 3 4题为第一组2 5 7题为第二组6 9 10题为第三组8题单独列出）"}',
    display_condition_json = 'null'
WHERE questionnaire_version_id = @version_id AND item_code = 'P3-10' AND deleted = 0;
UPDATE assessment_question_version
SET question_type = 'SINGLE_CHOICE',
    stem_text = 'Une journée de travail — blanc (10)',
    prompt_text = '请阅读上面的完整短文，为空格 (10) 选择最佳答案。',
    options_json = '[{"key":"A","label":"simple"},{"key":"B","label":"habituelle"},{"key":"C","label":"singulière"},{"key":"D","label":"prudente"}]',
    correct_answer_json = '["B"]',
    explanation_text = '10. 答案：B. habituel
#题目分析：
此题为英法同源词。法语 habituel = “习惯性的、通常的/常见的”，与英语 habitual 同源且语义基本重合，学生可借助英语正迁移理解词义。
干扰项 A “简单的”（simple）：语义上与“职场挫折”无直接关联，且与后文“明天会好转”缺乏逻辑呼应。学生可能因“简单的事容易过去”这一日常联想而误选，属于语义泛化干扰。
干扰项 C “独特的、奇特的”（singulière）：与上文 tout le monde peut vivre ce genre de déception（每个人都会经历这种失望）逻辑相反——人人都能经历的事，不可能是“独特的”，可快速排除。
干扰项 D “谨慎的”（prudente）：语义方向完全偏离，与“失望/挫折”的情感色彩不匹配，且修饰 situation 不自然，属于随机猜测干扰项。
（1 3 4题为第一组2 5 7题为第二组6 9 10题为第三组8题单独列出）',
    option_explanations_json = '{"A":"干扰项 A “简单的”（simple）：语义上与“职场挫折”无直接关联，且与后文“明天会好转”缺乏逻辑呼应。学生可能因“简单的事容易过去”这一日常联想而误选，属于语义泛化干扰。","C":"干扰项 C “独特的、奇特的”（singulière）：与上文 tout le monde peut vivre ce genre de déception（每个人都会经历这种失望）逻辑相反——人人都能经历的事，不可能是“独特的”，可快速排除。","D":"干扰项 D “谨慎的”（prudente）：语义方向完全偏离，与“失望/挫折”的情感色彩不匹配，且修饰 situation 不自然，属于随机猜测干扰项。","B":"#题目分析：\\n此题为英法同源词。法语 habituel = “习惯性的、通常的/常见的”，与英语 habitual 同源且语义基本重合，学生可借助英语正迁移理解词义。\\n（1 3 4题为第一组2 5 7题为第二组6 9 10题为第三组8题单独列出）"}',
    required_answer = true, weight = 1,
    transfer_category = 'COGNATE',
    context_level = 'CLOZE',
    construct_code = 'CONTEXT_REPAIR',
    target_word = 'habituel',
    display_condition_json = 'null',
    source_reference = 'questionnaire:P3-10',
    content_hash = '1d7efec1856a0959c576dad313cd73edf8b745e4eb63fa158951aa6e353b116c'
WHERE question_bank_id = @bank_id AND question_code = 'P3-10' AND version_no = 1 AND deleted = 0;

-- P4T1-01
UPDATE assessment_question q
JOIN assessment_questionnaire_item i ON i.assessment_question_id = q.id
SET q.question_type = 'SINGLE_CHOICE',
    q.stem_text = 'Quelle est l''idée principale du texte ?',
    q.prompt_text = '请阅读上面的完整短文，选择最佳答案。',
    q.options_json = '[{"key":"A","label":"Les usines sont bénéfiques pour l''air."},{"key":"B","label":"La pollution menace la planète et il faut agir vite."},{"key":"C","label":"Les incendies sont déjà partout."},{"key":"D","label":"Les animaux et les plantes n''ont aucun danger."}]',
    q.correct_answer_json = '["B"]',
    q.explanation_text = '1. Quelle est l’idée principale du texte ?
题型：主旨题
#题目分析：答案：B
•干扰项 A “工厂对空气有益”：与原文相反。原文提到 les fumées grises provenant des usines（工厂排放的灰色烟雾），暗示工厂是污染源。
• 干扰项 B “污染威胁地球，必须快速行动”：概括了全文核心。第一段讲污染问题，第二段讲解决措施，符合主旨。
• 干扰项 C “火灾已经到处都是”：原文说的是 en cas d’incendie éventuel（可能的火灾），并非已经发生。
• 干扰项 D “动物和植物没有任何危险”：与原文相反。原文提到火灾可能 détruire leurs habitats（摧毁它们的栖息地）。',
    q.score = 1,
    q.section_code = 'P4T1',
    q.required_answer = true,
    q.weight = 1,
    q.transfer_category = '',
    q.context_level = 'READING',
    q.construct_code = 'CONTEXT_REPAIR',
    q.target_word = '',
    q.option_explanations_json = '{"A":"•干扰项 A “工厂对空气有益”：与原文相反。原文提到 les fumées grises provenant des usines（工厂排放的灰色烟雾），暗示工厂是污染源。","B":"• 干扰项 B “污染威胁地球，必须快速行动”：概括了全文核心。第一段讲污染问题，第二段讲解决措施，符合主旨。","C":"• 干扰项 C “火灾已经到处都是”：原文说的是 en cas d’incendie éventuel（可能的火灾），并非已经发生。","D":"• 干扰项 D “动物和植物没有任何危险”：与原文相反。原文提到火灾可能 détruire leurs habitats（摧毁它们的栖息地）。"}',
    q.display_condition_json = 'null'
WHERE i.questionnaire_version_id = @version_id AND i.item_code = 'P4T1-01' AND i.deleted = 0 AND q.deleted = 0;
UPDATE assessment_questionnaire_item
SET required_answer = true, scored = true, weight = 1,
    transfer_category = '',
    context_level = 'READING',
    construct_code = 'CONTEXT_REPAIR',
    target_word = '',
    option_explanations_json = '{"A":"•干扰项 A “工厂对空气有益”：与原文相反。原文提到 les fumées grises provenant des usines（工厂排放的灰色烟雾），暗示工厂是污染源。","B":"• 干扰项 B “污染威胁地球，必须快速行动”：概括了全文核心。第一段讲污染问题，第二段讲解决措施，符合主旨。","C":"• 干扰项 C “火灾已经到处都是”：原文说的是 en cas d’incendie éventuel（可能的火灾），并非已经发生。","D":"• 干扰项 D “动物和植物没有任何危险”：与原文相反。原文提到火灾可能 détruire leurs habitats（摧毁它们的栖息地）。"}',
    display_condition_json = 'null'
WHERE questionnaire_version_id = @version_id AND item_code = 'P4T1-01' AND deleted = 0;
UPDATE assessment_question_version
SET question_type = 'SINGLE_CHOICE',
    stem_text = 'Quelle est l''idée principale du texte ?',
    prompt_text = '请阅读上面的完整短文，选择最佳答案。',
    options_json = '[{"key":"A","label":"Les usines sont bénéfiques pour l''air."},{"key":"B","label":"La pollution menace la planète et il faut agir vite."},{"key":"C","label":"Les incendies sont déjà partout."},{"key":"D","label":"Les animaux et les plantes n''ont aucun danger."}]',
    correct_answer_json = '["B"]',
    explanation_text = '1. Quelle est l’idée principale du texte ?
题型：主旨题
#题目分析：答案：B
•干扰项 A “工厂对空气有益”：与原文相反。原文提到 les fumées grises provenant des usines（工厂排放的灰色烟雾），暗示工厂是污染源。
• 干扰项 B “污染威胁地球，必须快速行动”：概括了全文核心。第一段讲污染问题，第二段讲解决措施，符合主旨。
• 干扰项 C “火灾已经到处都是”：原文说的是 en cas d’incendie éventuel（可能的火灾），并非已经发生。
• 干扰项 D “动物和植物没有任何危险”：与原文相反。原文提到火灾可能 détruire leurs habitats（摧毁它们的栖息地）。',
    option_explanations_json = '{"A":"•干扰项 A “工厂对空气有益”：与原文相反。原文提到 les fumées grises provenant des usines（工厂排放的灰色烟雾），暗示工厂是污染源。","B":"• 干扰项 B “污染威胁地球，必须快速行动”：概括了全文核心。第一段讲污染问题，第二段讲解决措施，符合主旨。","C":"• 干扰项 C “火灾已经到处都是”：原文说的是 en cas d’incendie éventuel（可能的火灾），并非已经发生。","D":"• 干扰项 D “动物和植物没有任何危险”：与原文相反。原文提到火灾可能 détruire leurs habitats（摧毁它们的栖息地）。"}',
    required_answer = true, weight = 1,
    transfer_category = '',
    context_level = 'READING',
    construct_code = 'CONTEXT_REPAIR',
    target_word = '',
    display_condition_json = 'null',
    source_reference = 'questionnaire:P4T1-01',
    content_hash = 'cd656a926046a558708c6af8378e82785f72ed3de461496c255d0791ea2c5de9'
WHERE question_bank_id = @bank_id AND question_code = 'P4T1-01' AND version_no = 1 AND deleted = 0;

-- P4T1-02
UPDATE assessment_question q
JOIN assessment_questionnaire_item i ON i.assessment_question_id = q.id
SET q.question_type = 'SINGLE_CHOICE',
    q.stem_text = 'Que peut-on déduire des gens qui « ignorent que la pollution peut aussi nuire aux animaux » ?',
    q.prompt_text = '请阅读上面的完整短文，选择最佳答案。',
    q.options_json = '[{"key":"A","label":"Ils ne savent pas que les animaux sont menacés par la pollution."},{"key":"B","label":"Ils négligent volontairement la protection des animaux."},{"key":"C","label":"Ils pensent que la pollution n''affecte que les humains."},{"key":"D","label":"Ils agissent déjà pour protéger l''environnement."}]',
    q.correct_answer_json = '["A"]',
    q.explanation_text = '2. Que peut-on déduire des gens qui « ignorent que la pollution peut aussi nuire aux animaux » ?
#题目分析：答案：A
题型：推断题（假朋友 ignorer 考察）
·干扰项 A “他们不知道动物受到污染的威胁”：法语 ignorer 的正确含义，是正确答案。
·干扰项 B “他们故意疏忽对动物的保护”：法语 négliger（主观疏忽）的含义，与 ignorer（不知道）形成对比，是强干扰项。
·干扰项 C “他们认为污染只影响人类”：学生若混淆“不知道”与“认为相反”，可能误选。
·干扰项 D “他们已经在为保护环境采取行动”：与原文逻辑相反（如果已经行动就不会“不知道”），是弱干扰项。',
    q.score = 1,
    q.section_code = 'P4T1',
    q.required_answer = true,
    q.weight = 1,
    q.transfer_category = 'FALSE_FRIEND',
    q.context_level = 'READING',
    q.construct_code = 'CONTEXT_REPAIR',
    q.target_word = 'ignorer',
    q.option_explanations_json = '{"A":"·干扰项 A “他们不知道动物受到污染的威胁”：法语 ignorer 的正确含义，是正确答案。","B":"·干扰项 B “他们故意疏忽对动物的保护”：法语 négliger（主观疏忽）的含义，与 ignorer（不知道）形成对比，是强干扰项。","C":"·干扰项 C “他们认为污染只影响人类”：学生若混淆“不知道”与“认为相反”，可能误选。","D":"·干扰项 D “他们已经在为保护环境采取行动”：与原文逻辑相反（如果已经行动就不会“不知道”），是弱干扰项。"}',
    q.display_condition_json = 'null'
WHERE i.questionnaire_version_id = @version_id AND i.item_code = 'P4T1-02' AND i.deleted = 0 AND q.deleted = 0;
UPDATE assessment_questionnaire_item
SET required_answer = true, scored = true, weight = 1,
    transfer_category = 'FALSE_FRIEND',
    context_level = 'READING',
    construct_code = 'CONTEXT_REPAIR',
    target_word = 'ignorer',
    option_explanations_json = '{"A":"·干扰项 A “他们不知道动物受到污染的威胁”：法语 ignorer 的正确含义，是正确答案。","B":"·干扰项 B “他们故意疏忽对动物的保护”：法语 négliger（主观疏忽）的含义，与 ignorer（不知道）形成对比，是强干扰项。","C":"·干扰项 C “他们认为污染只影响人类”：学生若混淆“不知道”与“认为相反”，可能误选。","D":"·干扰项 D “他们已经在为保护环境采取行动”：与原文逻辑相反（如果已经行动就不会“不知道”），是弱干扰项。"}',
    display_condition_json = 'null'
WHERE questionnaire_version_id = @version_id AND item_code = 'P4T1-02' AND deleted = 0;
UPDATE assessment_question_version
SET question_type = 'SINGLE_CHOICE',
    stem_text = 'Que peut-on déduire des gens qui « ignorent que la pollution peut aussi nuire aux animaux » ?',
    prompt_text = '请阅读上面的完整短文，选择最佳答案。',
    options_json = '[{"key":"A","label":"Ils ne savent pas que les animaux sont menacés par la pollution."},{"key":"B","label":"Ils négligent volontairement la protection des animaux."},{"key":"C","label":"Ils pensent que la pollution n''affecte que les humains."},{"key":"D","label":"Ils agissent déjà pour protéger l''environnement."}]',
    correct_answer_json = '["A"]',
    explanation_text = '2. Que peut-on déduire des gens qui « ignorent que la pollution peut aussi nuire aux animaux » ?
#题目分析：答案：A
题型：推断题（假朋友 ignorer 考察）
·干扰项 A “他们不知道动物受到污染的威胁”：法语 ignorer 的正确含义，是正确答案。
·干扰项 B “他们故意疏忽对动物的保护”：法语 négliger（主观疏忽）的含义，与 ignorer（不知道）形成对比，是强干扰项。
·干扰项 C “他们认为污染只影响人类”：学生若混淆“不知道”与“认为相反”，可能误选。
·干扰项 D “他们已经在为保护环境采取行动”：与原文逻辑相反（如果已经行动就不会“不知道”），是弱干扰项。',
    option_explanations_json = '{"A":"·干扰项 A “他们不知道动物受到污染的威胁”：法语 ignorer 的正确含义，是正确答案。","B":"·干扰项 B “他们故意疏忽对动物的保护”：法语 négliger（主观疏忽）的含义，与 ignorer（不知道）形成对比，是强干扰项。","C":"·干扰项 C “他们认为污染只影响人类”：学生若混淆“不知道”与“认为相反”，可能误选。","D":"·干扰项 D “他们已经在为保护环境采取行动”：与原文逻辑相反（如果已经行动就不会“不知道”），是弱干扰项。"}',
    required_answer = true, weight = 1,
    transfer_category = 'FALSE_FRIEND',
    context_level = 'READING',
    construct_code = 'CONTEXT_REPAIR',
    target_word = 'ignorer',
    display_condition_json = 'null',
    source_reference = 'questionnaire:P4T1-02',
    content_hash = '95a402d9bf1df7de72fdfa69dd79a7f267d944597a8882e6055ef189a0dfbca8'
WHERE question_bank_id = @bank_id AND question_code = 'P4T1-02' AND version_no = 1 AND deleted = 0;

-- P4T1-03
UPDATE assessment_question q
JOIN assessment_questionnaire_item i ON i.assessment_question_id = q.id
SET q.question_type = 'SINGLE_CHOICE',
    q.stem_text = 'Si la qualité de l''air devient mauvaise :',
    q.prompt_text = '请阅读上面的完整短文，选择最佳答案。',
    q.options_json = '[{"key":"A","label":"nombreuses personnes ne s''en soucieront pas."},{"key":"B","label":"certaines personnes raisonnables tomberont malade."},{"key":"C","label":"certaines personnes vulnérables se sentiront mal à l''aise."},{"key":"D","label":"certaines personnes seront plus tristes qu''avant."}]',
    q.correct_answer_json = '["C"]',
    q.explanation_text = '3. Si la qualité de l’air devient mauvais :
题型：细节理解题
#题目分析：答案：C
• 干扰项 A “很多人不会在意”：原文未提及，且与 sensibles（敏感的）语义相反。
• 干扰项 B “一些理性的人会生病”：raisonnables（理性的）是对法语 sensible 的误译（英语 sensible = 明智的/理性的），是假朋友负迁移干扰。
• 干扰项 C “一些敏感的人会感到不适”：vulnérables与 sensibles 同义，se sentir mal à l’aise 对应原文 tousser（咳嗽），是正确答案。
• 干扰项 D “一些人会比以前更难过”：过度理解。原文只提到咳嗽，未涉及情绪变化。',
    q.score = 1,
    q.section_code = 'P4T1',
    q.required_answer = true,
    q.weight = 1,
    q.transfer_category = 'FALSE_FRIEND',
    q.context_level = 'READING',
    q.construct_code = 'CONTEXT_REPAIR',
    q.target_word = 'sensible',
    q.option_explanations_json = '{"A":"• 干扰项 A “很多人不会在意”：原文未提及，且与 sensibles（敏感的）语义相反。","B":"• 干扰项 B “一些理性的人会生病”：raisonnables（理性的）是对法语 sensible 的误译（英语 sensible = 明智的/理性的），是假朋友负迁移干扰。","C":"• 干扰项 C “一些敏感的人会感到不适”：vulnérables与 sensibles 同义，se sentir mal à l’aise 对应原文 tousser（咳嗽），是正确答案。","D":"• 干扰项 D “一些人会比以前更难过”：过度理解。原文只提到咳嗽，未涉及情绪变化。"}',
    q.display_condition_json = 'null'
WHERE i.questionnaire_version_id = @version_id AND i.item_code = 'P4T1-03' AND i.deleted = 0 AND q.deleted = 0;
UPDATE assessment_questionnaire_item
SET required_answer = true, scored = true, weight = 1,
    transfer_category = 'FALSE_FRIEND',
    context_level = 'READING',
    construct_code = 'CONTEXT_REPAIR',
    target_word = 'sensible',
    option_explanations_json = '{"A":"• 干扰项 A “很多人不会在意”：原文未提及，且与 sensibles（敏感的）语义相反。","B":"• 干扰项 B “一些理性的人会生病”：raisonnables（理性的）是对法语 sensible 的误译（英语 sensible = 明智的/理性的），是假朋友负迁移干扰。","C":"• 干扰项 C “一些敏感的人会感到不适”：vulnérables与 sensibles 同义，se sentir mal à l’aise 对应原文 tousser（咳嗽），是正确答案。","D":"• 干扰项 D “一些人会比以前更难过”：过度理解。原文只提到咳嗽，未涉及情绪变化。"}',
    display_condition_json = 'null'
WHERE questionnaire_version_id = @version_id AND item_code = 'P4T1-03' AND deleted = 0;
UPDATE assessment_question_version
SET question_type = 'SINGLE_CHOICE',
    stem_text = 'Si la qualité de l''air devient mauvaise :',
    prompt_text = '请阅读上面的完整短文，选择最佳答案。',
    options_json = '[{"key":"A","label":"nombreuses personnes ne s''en soucieront pas."},{"key":"B","label":"certaines personnes raisonnables tomberont malade."},{"key":"C","label":"certaines personnes vulnérables se sentiront mal à l''aise."},{"key":"D","label":"certaines personnes seront plus tristes qu''avant."}]',
    correct_answer_json = '["C"]',
    explanation_text = '3. Si la qualité de l’air devient mauvais :
题型：细节理解题
#题目分析：答案：C
• 干扰项 A “很多人不会在意”：原文未提及，且与 sensibles（敏感的）语义相反。
• 干扰项 B “一些理性的人会生病”：raisonnables（理性的）是对法语 sensible 的误译（英语 sensible = 明智的/理性的），是假朋友负迁移干扰。
• 干扰项 C “一些敏感的人会感到不适”：vulnérables与 sensibles 同义，se sentir mal à l’aise 对应原文 tousser（咳嗽），是正确答案。
• 干扰项 D “一些人会比以前更难过”：过度理解。原文只提到咳嗽，未涉及情绪变化。',
    option_explanations_json = '{"A":"• 干扰项 A “很多人不会在意”：原文未提及，且与 sensibles（敏感的）语义相反。","B":"• 干扰项 B “一些理性的人会生病”：raisonnables（理性的）是对法语 sensible 的误译（英语 sensible = 明智的/理性的），是假朋友负迁移干扰。","C":"• 干扰项 C “一些敏感的人会感到不适”：vulnérables与 sensibles 同义，se sentir mal à l’aise 对应原文 tousser（咳嗽），是正确答案。","D":"• 干扰项 D “一些人会比以前更难过”：过度理解。原文只提到咳嗽，未涉及情绪变化。"}',
    required_answer = true, weight = 1,
    transfer_category = 'FALSE_FRIEND',
    context_level = 'READING',
    construct_code = 'CONTEXT_REPAIR',
    target_word = 'sensible',
    display_condition_json = 'null',
    source_reference = 'questionnaire:P4T1-03',
    content_hash = 'f8a6c94d05d912e7afd7a3cfcffe86928d2e8db59b04c65a16bb9e25eb779038'
WHERE question_bank_id = @bank_id AND question_code = 'P4T1-03' AND version_no = 1 AND deleted = 0;

-- P4T1-04
UPDATE assessment_question q
JOIN assessment_questionnaire_item i ON i.assessment_question_id = q.id
SET q.question_type = 'SINGLE_CHOICE',
    q.stem_text = 'Que signifie « un éventuel incendie » ?',
    q.prompt_text = '请阅读上面的完整短文，选择最佳答案。',
    q.options_json = '[{"key":"A","label":"un incendie possible mais pas certain"},{"key":"B","label":"un incendie inévitable et sûr"},{"key":"C","label":"un incendie impossible"},{"key":"D","label":"un incendie déjà terminé"}]',
    q.correct_answer_json = '["A"]',
    q.explanation_text = '4. Que signifie « un éventuel incendie » ?
题型：词汇理解题（假朋友考察）
#题目分析：答案 A
此题为假朋友（同形异义词）辨析题。法语 éventuel = “可能的、或许会发生的”，强调不确定性；英语形似词 eventual = “最终的、结局的”，强调必然性。二者词形高度相似但核心语义不同。
干扰项 B “un incendie inévitable et sûr”（不可避免且确定的火灾）：此为英语 eventual 的语义特征（“最终的”隐含“必然会发生”），受英语负迁移影响的学生容易误选。但法语 éventuel 不包含“必然”之义。
干扰项 C “un incendie impossible”（不可能的火灾）：与 éventuel 的“可能的”语义完全相反，属于极端错误选项。
干扰项 D “un incendie déjà terminé”（已经结束的火灾）：与英语 eventual 的“最终的”在时间维度上部分混淆，但法语 éventuel 完全不涉及“已发生”或“已完成”的时间概念。',
    q.score = 1,
    q.section_code = 'P4T1',
    q.required_answer = true,
    q.weight = 1,
    q.transfer_category = 'FALSE_FRIEND',
    q.context_level = 'READING',
    q.construct_code = 'CONTEXT_REPAIR',
    q.target_word = 'éventuel',
    q.option_explanations_json = '{"B":"干扰项 B “un incendie inévitable et sûr”（不可避免且确定的火灾）：此为英语 eventual 的语义特征（“最终的”隐含“必然会发生”），受英语负迁移影响的学生容易误选。但法语 éventuel 不包含“必然”之义。","C":"干扰项 C “un incendie impossible”（不可能的火灾）：与 éventuel 的“可能的”语义完全相反，属于极端错误选项。","D":"干扰项 D “un incendie déjà terminé”（已经结束的火灾）：与英语 eventual 的“最终的”在时间维度上部分混淆，但法语 éventuel 完全不涉及“已发生”或“已完成”的时间概念。","A":"此题为假朋友（同形异义词）辨析题。法语 éventuel = “可能的、或许会发生的”，强调不确定性；英语形似词 eventual = “最终的、结局的”，强调必然性。二者词形高度相似但核心语义不同。"}',
    q.display_condition_json = 'null'
WHERE i.questionnaire_version_id = @version_id AND i.item_code = 'P4T1-04' AND i.deleted = 0 AND q.deleted = 0;
UPDATE assessment_questionnaire_item
SET required_answer = true, scored = true, weight = 1,
    transfer_category = 'FALSE_FRIEND',
    context_level = 'READING',
    construct_code = 'CONTEXT_REPAIR',
    target_word = 'éventuel',
    option_explanations_json = '{"B":"干扰项 B “un incendie inévitable et sûr”（不可避免且确定的火灾）：此为英语 eventual 的语义特征（“最终的”隐含“必然会发生”），受英语负迁移影响的学生容易误选。但法语 éventuel 不包含“必然”之义。","C":"干扰项 C “un incendie impossible”（不可能的火灾）：与 éventuel 的“可能的”语义完全相反，属于极端错误选项。","D":"干扰项 D “un incendie déjà terminé”（已经结束的火灾）：与英语 eventual 的“最终的”在时间维度上部分混淆，但法语 éventuel 完全不涉及“已发生”或“已完成”的时间概念。","A":"此题为假朋友（同形异义词）辨析题。法语 éventuel = “可能的、或许会发生的”，强调不确定性；英语形似词 eventual = “最终的、结局的”，强调必然性。二者词形高度相似但核心语义不同。"}',
    display_condition_json = 'null'
WHERE questionnaire_version_id = @version_id AND item_code = 'P4T1-04' AND deleted = 0;
UPDATE assessment_question_version
SET question_type = 'SINGLE_CHOICE',
    stem_text = 'Que signifie « un éventuel incendie » ?',
    prompt_text = '请阅读上面的完整短文，选择最佳答案。',
    options_json = '[{"key":"A","label":"un incendie possible mais pas certain"},{"key":"B","label":"un incendie inévitable et sûr"},{"key":"C","label":"un incendie impossible"},{"key":"D","label":"un incendie déjà terminé"}]',
    correct_answer_json = '["A"]',
    explanation_text = '4. Que signifie « un éventuel incendie » ?
题型：词汇理解题（假朋友考察）
#题目分析：答案 A
此题为假朋友（同形异义词）辨析题。法语 éventuel = “可能的、或许会发生的”，强调不确定性；英语形似词 eventual = “最终的、结局的”，强调必然性。二者词形高度相似但核心语义不同。
干扰项 B “un incendie inévitable et sûr”（不可避免且确定的火灾）：此为英语 eventual 的语义特征（“最终的”隐含“必然会发生”），受英语负迁移影响的学生容易误选。但法语 éventuel 不包含“必然”之义。
干扰项 C “un incendie impossible”（不可能的火灾）：与 éventuel 的“可能的”语义完全相反，属于极端错误选项。
干扰项 D “un incendie déjà terminé”（已经结束的火灾）：与英语 eventual 的“最终的”在时间维度上部分混淆，但法语 éventuel 完全不涉及“已发生”或“已完成”的时间概念。',
    option_explanations_json = '{"B":"干扰项 B “un incendie inévitable et sûr”（不可避免且确定的火灾）：此为英语 eventual 的语义特征（“最终的”隐含“必然会发生”），受英语负迁移影响的学生容易误选。但法语 éventuel 不包含“必然”之义。","C":"干扰项 C “un incendie impossible”（不可能的火灾）：与 éventuel 的“可能的”语义完全相反，属于极端错误选项。","D":"干扰项 D “un incendie déjà terminé”（已经结束的火灾）：与英语 eventual 的“最终的”在时间维度上部分混淆，但法语 éventuel 完全不涉及“已发生”或“已完成”的时间概念。","A":"此题为假朋友（同形异义词）辨析题。法语 éventuel = “可能的、或许会发生的”，强调不确定性；英语形似词 eventual = “最终的、结局的”，强调必然性。二者词形高度相似但核心语义不同。"}',
    required_answer = true, weight = 1,
    transfer_category = 'FALSE_FRIEND',
    context_level = 'READING',
    construct_code = 'CONTEXT_REPAIR',
    target_word = 'éventuel',
    display_condition_json = 'null',
    source_reference = 'questionnaire:P4T1-04',
    content_hash = '118a9efab32a5218356d0405cdfd84ccbd27236923c030ae8756523df699f62f'
WHERE question_bank_id = @bank_id AND question_code = 'P4T1-04' AND version_no = 1 AND deleted = 0;

-- P4T1-05
UPDATE assessment_question q
JOIN assessment_questionnaire_item i ON i.assessment_question_id = q.id
SET q.question_type = 'SINGLE_CHOICE',
    q.stem_text = 'Que signifie « Agir maintenant est essentiel pour l''avenir » ?',
    q.prompt_text = '请阅读上面的完整短文，选择最佳答案。',
    q.options_json = '[{"key":"A","label":"Agir maintenant est utile pour l''avenir"},{"key":"B","label":"Agir maintenant est dangereux pour l''avenir"},{"key":"C","label":"Agir maintenant est premier pour l''avenir"},{"key":"D","label":"Agir maintenant est nécessaire pour l''avenir"}]',
    q.correct_answer_json = '["D"]',
    q.explanation_text = '5. Que signifie « Agir maintenant est essentiel pour l’avenir » ?
题型：词汇理解题（同源词考察）
#题目分析：答案：D
• 干扰项 A “现在行动对未来有用”：utile（有用的）与 essentiel（必要的）语义相近但不完全相同，是弱干扰项。
• 干扰项 B “现在行动对未来有危险”：dangereux（危险的）与原文语义相反。
• 干扰项 C “现在行动是未来的首要之事”：premier（第一位的）与 essentiel（必要的）语义部分重叠，但 premier 更侧重顺序，是弱干扰项。
• 干扰项 D “现在行动对未来是必要的”：nécessaire（必要的）与 essentiel 同义，是正确答案。',
    q.score = 1,
    q.section_code = 'P4T1',
    q.required_answer = true,
    q.weight = 1,
    q.transfer_category = 'COGNATE',
    q.context_level = 'READING',
    q.construct_code = 'CONTEXT_REPAIR',
    q.target_word = 'essentiel',
    q.option_explanations_json = '{"A":"• 干扰项 A “现在行动对未来有用”：utile（有用的）与 essentiel（必要的）语义相近但不完全相同，是弱干扰项。","B":"• 干扰项 B “现在行动对未来有危险”：dangereux（危险的）与原文语义相反。","C":"• 干扰项 C “现在行动是未来的首要之事”：premier（第一位的）与 essentiel（必要的）语义部分重叠，但 premier 更侧重顺序，是弱干扰项。","D":"• 干扰项 D “现在行动对未来是必要的”：nécessaire（必要的）与 essentiel 同义，是正确答案。"}',
    q.display_condition_json = 'null'
WHERE i.questionnaire_version_id = @version_id AND i.item_code = 'P4T1-05' AND i.deleted = 0 AND q.deleted = 0;
UPDATE assessment_questionnaire_item
SET required_answer = true, scored = true, weight = 1,
    transfer_category = 'COGNATE',
    context_level = 'READING',
    construct_code = 'CONTEXT_REPAIR',
    target_word = 'essentiel',
    option_explanations_json = '{"A":"• 干扰项 A “现在行动对未来有用”：utile（有用的）与 essentiel（必要的）语义相近但不完全相同，是弱干扰项。","B":"• 干扰项 B “现在行动对未来有危险”：dangereux（危险的）与原文语义相反。","C":"• 干扰项 C “现在行动是未来的首要之事”：premier（第一位的）与 essentiel（必要的）语义部分重叠，但 premier 更侧重顺序，是弱干扰项。","D":"• 干扰项 D “现在行动对未来是必要的”：nécessaire（必要的）与 essentiel 同义，是正确答案。"}',
    display_condition_json = 'null'
WHERE questionnaire_version_id = @version_id AND item_code = 'P4T1-05' AND deleted = 0;
UPDATE assessment_question_version
SET question_type = 'SINGLE_CHOICE',
    stem_text = 'Que signifie « Agir maintenant est essentiel pour l''avenir » ?',
    prompt_text = '请阅读上面的完整短文，选择最佳答案。',
    options_json = '[{"key":"A","label":"Agir maintenant est utile pour l''avenir"},{"key":"B","label":"Agir maintenant est dangereux pour l''avenir"},{"key":"C","label":"Agir maintenant est premier pour l''avenir"},{"key":"D","label":"Agir maintenant est nécessaire pour l''avenir"}]',
    correct_answer_json = '["D"]',
    explanation_text = '5. Que signifie « Agir maintenant est essentiel pour l’avenir » ?
题型：词汇理解题（同源词考察）
#题目分析：答案：D
• 干扰项 A “现在行动对未来有用”：utile（有用的）与 essentiel（必要的）语义相近但不完全相同，是弱干扰项。
• 干扰项 B “现在行动对未来有危险”：dangereux（危险的）与原文语义相反。
• 干扰项 C “现在行动是未来的首要之事”：premier（第一位的）与 essentiel（必要的）语义部分重叠，但 premier 更侧重顺序，是弱干扰项。
• 干扰项 D “现在行动对未来是必要的”：nécessaire（必要的）与 essentiel 同义，是正确答案。',
    option_explanations_json = '{"A":"• 干扰项 A “现在行动对未来有用”：utile（有用的）与 essentiel（必要的）语义相近但不完全相同，是弱干扰项。","B":"• 干扰项 B “现在行动对未来有危险”：dangereux（危险的）与原文语义相反。","C":"• 干扰项 C “现在行动是未来的首要之事”：premier（第一位的）与 essentiel（必要的）语义部分重叠，但 premier 更侧重顺序，是弱干扰项。","D":"• 干扰项 D “现在行动对未来是必要的”：nécessaire（必要的）与 essentiel 同义，是正确答案。"}',
    required_answer = true, weight = 1,
    transfer_category = 'COGNATE',
    context_level = 'READING',
    construct_code = 'CONTEXT_REPAIR',
    target_word = 'essentiel',
    display_condition_json = 'null',
    source_reference = 'questionnaire:P4T1-05',
    content_hash = '351628ff15b7c129d0b729f1b3489d7b82c416b2003f3ebfff9478024083fb35'
WHERE question_bank_id = @bank_id AND question_code = 'P4T1-05' AND version_no = 1 AND deleted = 0;

-- P4T2-01
UPDATE assessment_question q
JOIN assessment_questionnaire_item i ON i.assessment_question_id = q.id
SET q.question_type = 'SINGLE_CHOICE',
    q.stem_text = 'Quelle est l''idée principale du texte ?',
    q.prompt_text = '请阅读上面的完整短文，选择最佳答案。',
    q.options_json = '[{"key":"A","label":"Sophie a démissionné parce qu''elle n''aimait pas son patron."},{"key":"B","label":"Sophie a quitté son travail pour ouvrir une librairie."},{"key":"C","label":"Sophie a déjà ouvert une librairie mais elle regrette son choix."},{"key":"D","label":"Sophie travaille toujours dans une entreprise internationale."}]',
    q.correct_answer_json = '["B"]',
    q.explanation_text = '1. Quelle est l’idée principale du texte ?
题型：主旨题
#题目分析：答案：B
• 干扰项 A “Sophie 辞职因为她不喜欢老板”：原文未提及老板或辞职原因，是过度推断。
• 干扰项 B “Sophie 辞去工作为了开一家书店”：概括了全文核心（辞职 → 梦想开店 → 正在培训）。
• 干扰项 C “Sophie 开了一家书店但后悔她的选择”：原文未提及后悔，相反她说这段经历 très stimulante（非常振奋人心）。
• 干扰项 D “Sophie 仍然在一家国际公司工作”：与原文 a quitté son poste（已离职）矛盾。',
    q.score = 1,
    q.section_code = 'P4T2',
    q.required_answer = true,
    q.weight = 1,
    q.transfer_category = '',
    q.context_level = 'READING',
    q.construct_code = 'CONTEXT_REPAIR',
    q.target_word = '',
    q.option_explanations_json = '{"A":"• 干扰项 A “Sophie 辞职因为她不喜欢老板”：原文未提及老板或辞职原因，是过度推断。","B":"• 干扰项 B “Sophie 辞去工作为了开一家书店”：概括了全文核心（辞职 → 梦想开店 → 正在培训）。","C":"• 干扰项 C “Sophie 开了一家书店但后悔她的选择”：原文未提及后悔，相反她说这段经历 très stimulante（非常振奋人心）。","D":"• 干扰项 D “Sophie 仍然在一家国际公司工作”：与原文 a quitté son poste（已离职）矛盾。"}',
    q.display_condition_json = 'null'
WHERE i.questionnaire_version_id = @version_id AND i.item_code = 'P4T2-01' AND i.deleted = 0 AND q.deleted = 0;
UPDATE assessment_questionnaire_item
SET required_answer = true, scored = true, weight = 1,
    transfer_category = '',
    context_level = 'READING',
    construct_code = 'CONTEXT_REPAIR',
    target_word = '',
    option_explanations_json = '{"A":"• 干扰项 A “Sophie 辞职因为她不喜欢老板”：原文未提及老板或辞职原因，是过度推断。","B":"• 干扰项 B “Sophie 辞去工作为了开一家书店”：概括了全文核心（辞职 → 梦想开店 → 正在培训）。","C":"• 干扰项 C “Sophie 开了一家书店但后悔她的选择”：原文未提及后悔，相反她说这段经历 très stimulante（非常振奋人心）。","D":"• 干扰项 D “Sophie 仍然在一家国际公司工作”：与原文 a quitté son poste（已离职）矛盾。"}',
    display_condition_json = 'null'
WHERE questionnaire_version_id = @version_id AND item_code = 'P4T2-01' AND deleted = 0;
UPDATE assessment_question_version
SET question_type = 'SINGLE_CHOICE',
    stem_text = 'Quelle est l''idée principale du texte ?',
    prompt_text = '请阅读上面的完整短文，选择最佳答案。',
    options_json = '[{"key":"A","label":"Sophie a démissionné parce qu''elle n''aimait pas son patron."},{"key":"B","label":"Sophie a quitté son travail pour ouvrir une librairie."},{"key":"C","label":"Sophie a déjà ouvert une librairie mais elle regrette son choix."},{"key":"D","label":"Sophie travaille toujours dans une entreprise internationale."}]',
    correct_answer_json = '["B"]',
    explanation_text = '1. Quelle est l’idée principale du texte ?
题型：主旨题
#题目分析：答案：B
• 干扰项 A “Sophie 辞职因为她不喜欢老板”：原文未提及老板或辞职原因，是过度推断。
• 干扰项 B “Sophie 辞去工作为了开一家书店”：概括了全文核心（辞职 → 梦想开店 → 正在培训）。
• 干扰项 C “Sophie 开了一家书店但后悔她的选择”：原文未提及后悔，相反她说这段经历 très stimulante（非常振奋人心）。
• 干扰项 D “Sophie 仍然在一家国际公司工作”：与原文 a quitté son poste（已离职）矛盾。',
    option_explanations_json = '{"A":"• 干扰项 A “Sophie 辞职因为她不喜欢老板”：原文未提及老板或辞职原因，是过度推断。","B":"• 干扰项 B “Sophie 辞去工作为了开一家书店”：概括了全文核心（辞职 → 梦想开店 → 正在培训）。","C":"• 干扰项 C “Sophie 开了一家书店但后悔她的选择”：原文未提及后悔，相反她说这段经历 très stimulante（非常振奋人心）。","D":"• 干扰项 D “Sophie 仍然在一家国际公司工作”：与原文 a quitté son poste（已离职）矛盾。"}',
    required_answer = true, weight = 1,
    transfer_category = '',
    context_level = 'READING',
    construct_code = 'CONTEXT_REPAIR',
    target_word = '',
    display_condition_json = 'null',
    source_reference = 'questionnaire:P4T2-01',
    content_hash = '789d43c3f1d538038d3b6592bd0dc2ee96d8ca7bd3e3417f6bd23ed6c8c51724'
WHERE question_bank_id = @bank_id AND question_code = 'P4T2-01' AND version_no = 1 AND deleted = 0;

-- P4T2-02
UPDATE assessment_question q
JOIN assessment_questionnaire_item i ON i.assessment_question_id = q.id
SET q.question_type = 'SINGLE_CHOICE',
    q.stem_text = 'À quel âge Sophie a-t-elle probablement commencé à rêver d’ouvrir une librairie ?',
    q.prompt_text = '请阅读上面的完整短文，选择最佳答案。',
    q.options_json = '[{"key":"A","label":"entre 6 et 10 ans"},{"key":"B","label":"entre 11 et 15 ans"},{"key":"C","label":"entre 18 et 22 ans"},{"key":"D","label":"après 25 ans"}]',
    q.correct_answer_json = '["B"]',
    q.explanation_text = '2. À quel âge Sophie a-t-elle probablement commencé à rêver d’ouvrir une librairie ?
题型：推断题（假朋友考察）
#题目分析：答案：B
• 干扰项 A “6–10岁”：对应小学阶段（école primaire）。法语中小学不称 collège。
• 干扰项 B “11–15岁”：对应 collège（初中阶段），是正确答案。考察学生对法语教育体系词汇 collège的理解。
• 干扰项 C “18–22岁”：对应大学（université）或高中最后一年（lycée），与 collège 不符，英语college可能导致混淆。
• 干扰项 D “25岁以后”：明显偏大，与 collège 的年龄范围不符。',
    q.score = 1,
    q.section_code = 'P4T2',
    q.required_answer = true,
    q.weight = 1,
    q.transfer_category = 'FALSE_FRIEND',
    q.context_level = 'READING',
    q.construct_code = 'CONTEXT_REPAIR',
    q.target_word = 'collège',
    q.option_explanations_json = '{"A":"• 干扰项 A “6–10岁”：对应小学阶段（école primaire）。法语中小学不称 collège。","B":"• 干扰项 B “11–15岁”：对应 collège（初中阶段），是正确答案。考察学生对法语教育体系词汇 collège的理解。","C":"• 干扰项 C “18–22岁”：对应大学（université）或高中最后一年（lycée），与 collège 不符，英语college可能导致混淆。","D":"• 干扰项 D “25岁以后”：明显偏大，与 collège 的年龄范围不符。"}',
    q.display_condition_json = 'null'
WHERE i.questionnaire_version_id = @version_id AND i.item_code = 'P4T2-02' AND i.deleted = 0 AND q.deleted = 0;
UPDATE assessment_questionnaire_item
SET required_answer = true, scored = true, weight = 1,
    transfer_category = 'FALSE_FRIEND',
    context_level = 'READING',
    construct_code = 'CONTEXT_REPAIR',
    target_word = 'collège',
    option_explanations_json = '{"A":"• 干扰项 A “6–10岁”：对应小学阶段（école primaire）。法语中小学不称 collège。","B":"• 干扰项 B “11–15岁”：对应 collège（初中阶段），是正确答案。考察学生对法语教育体系词汇 collège的理解。","C":"• 干扰项 C “18–22岁”：对应大学（université）或高中最后一年（lycée），与 collège 不符，英语college可能导致混淆。","D":"• 干扰项 D “25岁以后”：明显偏大，与 collège 的年龄范围不符。"}',
    display_condition_json = 'null'
WHERE questionnaire_version_id = @version_id AND item_code = 'P4T2-02' AND deleted = 0;
UPDATE assessment_question_version
SET question_type = 'SINGLE_CHOICE',
    stem_text = 'À quel âge Sophie a-t-elle probablement commencé à rêver d’ouvrir une librairie ?',
    prompt_text = '请阅读上面的完整短文，选择最佳答案。',
    options_json = '[{"key":"A","label":"entre 6 et 10 ans"},{"key":"B","label":"entre 11 et 15 ans"},{"key":"C","label":"entre 18 et 22 ans"},{"key":"D","label":"après 25 ans"}]',
    correct_answer_json = '["B"]',
    explanation_text = '2. À quel âge Sophie a-t-elle probablement commencé à rêver d’ouvrir une librairie ?
题型：推断题（假朋友考察）
#题目分析：答案：B
• 干扰项 A “6–10岁”：对应小学阶段（école primaire）。法语中小学不称 collège。
• 干扰项 B “11–15岁”：对应 collège（初中阶段），是正确答案。考察学生对法语教育体系词汇 collège的理解。
• 干扰项 C “18–22岁”：对应大学（université）或高中最后一年（lycée），与 collège 不符，英语college可能导致混淆。
• 干扰项 D “25岁以后”：明显偏大，与 collège 的年龄范围不符。',
    option_explanations_json = '{"A":"• 干扰项 A “6–10岁”：对应小学阶段（école primaire）。法语中小学不称 collège。","B":"• 干扰项 B “11–15岁”：对应 collège（初中阶段），是正确答案。考察学生对法语教育体系词汇 collège的理解。","C":"• 干扰项 C “18–22岁”：对应大学（université）或高中最后一年（lycée），与 collège 不符，英语college可能导致混淆。","D":"• 干扰项 D “25岁以后”：明显偏大，与 collège 的年龄范围不符。"}',
    required_answer = true, weight = 1,
    transfer_category = 'FALSE_FRIEND',
    context_level = 'READING',
    construct_code = 'CONTEXT_REPAIR',
    target_word = 'collège',
    display_condition_json = 'null',
    source_reference = 'questionnaire:P4T2-02',
    content_hash = '4ceb9b76e889887a94858ae415404b3ffedbf02b25ccd60e8c19f9b60f2d29ec'
WHERE question_bank_id = @bank_id AND question_code = 'P4T2-02' AND version_no = 1 AND deleted = 0;

-- P4T2-03
UPDATE assessment_question q
JOIN assessment_questionnaire_item i ON i.assessment_question_id = q.id
SET q.question_type = 'SINGLE_CHOICE',
    q.stem_text = 'Que signifie « formation » dans ce texte ?',
    q.prompt_text = '请阅读上面的完整短文，选择最佳答案。',
    q.options_json = '[{"key":"A","label":"une composition"},{"key":"B","label":"un stage professionnel"},{"key":"C","label":"un parcours d''études"},{"key":"D","label":"un voyage d''études"}]',
    q.correct_answer_json = '["C"]',
    q.explanation_text = '3. Que signifie « formation » dans ce texte ?
题型：词汇理解题（假朋友考察）
#题目分析：答案：C
• 干扰项 A “构成”（une composition）：英语 formation 的常见翻译（如地质构成、军队编队），是强干扰项（负迁移）。
• 干扰项 B “专业实习”（un stage professionnel）：与 formation 语义相近但不同（实习是培训的一种形式）。
• 干扰项 C “学习课程 / 培训”（un parcours d’études）：后文提到 étudier beaucoup de matières（学习很多科目），可推断 formation 在此指“课程/培训”，是正确答案。
• 干扰项 D “游学”（un voyage d’études）：语义相关但过于具体，原文未提及旅行。',
    q.score = 1,
    q.section_code = 'P4T2',
    q.required_answer = true,
    q.weight = 1,
    q.transfer_category = 'FALSE_FRIEND',
    q.context_level = 'READING',
    q.construct_code = 'CONTEXT_REPAIR',
    q.target_word = 'formation',
    q.option_explanations_json = '{"A":"• 干扰项 A “构成”（une composition）：英语 formation 的常见翻译（如地质构成、军队编队），是强干扰项（负迁移）。","B":"• 干扰项 B “专业实习”（un stage professionnel）：与 formation 语义相近但不同（实习是培训的一种形式）。","C":"• 干扰项 C “学习课程 / 培训”（un parcours d’études）：后文提到 étudier beaucoup de matières（学习很多科目），可推断 formation 在此指“课程/培训”，是正确答案。","D":"• 干扰项 D “游学”（un voyage d’études）：语义相关但过于具体，原文未提及旅行。"}',
    q.display_condition_json = 'null'
WHERE i.questionnaire_version_id = @version_id AND i.item_code = 'P4T2-03' AND i.deleted = 0 AND q.deleted = 0;
UPDATE assessment_questionnaire_item
SET required_answer = true, scored = true, weight = 1,
    transfer_category = 'FALSE_FRIEND',
    context_level = 'READING',
    construct_code = 'CONTEXT_REPAIR',
    target_word = 'formation',
    option_explanations_json = '{"A":"• 干扰项 A “构成”（une composition）：英语 formation 的常见翻译（如地质构成、军队编队），是强干扰项（负迁移）。","B":"• 干扰项 B “专业实习”（un stage professionnel）：与 formation 语义相近但不同（实习是培训的一种形式）。","C":"• 干扰项 C “学习课程 / 培训”（un parcours d’études）：后文提到 étudier beaucoup de matières（学习很多科目），可推断 formation 在此指“课程/培训”，是正确答案。","D":"• 干扰项 D “游学”（un voyage d’études）：语义相关但过于具体，原文未提及旅行。"}',
    display_condition_json = 'null'
WHERE questionnaire_version_id = @version_id AND item_code = 'P4T2-03' AND deleted = 0;
UPDATE assessment_question_version
SET question_type = 'SINGLE_CHOICE',
    stem_text = 'Que signifie « formation » dans ce texte ?',
    prompt_text = '请阅读上面的完整短文，选择最佳答案。',
    options_json = '[{"key":"A","label":"une composition"},{"key":"B","label":"un stage professionnel"},{"key":"C","label":"un parcours d''études"},{"key":"D","label":"un voyage d''études"}]',
    correct_answer_json = '["C"]',
    explanation_text = '3. Que signifie « formation » dans ce texte ?
题型：词汇理解题（假朋友考察）
#题目分析：答案：C
• 干扰项 A “构成”（une composition）：英语 formation 的常见翻译（如地质构成、军队编队），是强干扰项（负迁移）。
• 干扰项 B “专业实习”（un stage professionnel）：与 formation 语义相近但不同（实习是培训的一种形式）。
• 干扰项 C “学习课程 / 培训”（un parcours d’études）：后文提到 étudier beaucoup de matières（学习很多科目），可推断 formation 在此指“课程/培训”，是正确答案。
• 干扰项 D “游学”（un voyage d’études）：语义相关但过于具体，原文未提及旅行。',
    option_explanations_json = '{"A":"• 干扰项 A “构成”（une composition）：英语 formation 的常见翻译（如地质构成、军队编队），是强干扰项（负迁移）。","B":"• 干扰项 B “专业实习”（un stage professionnel）：与 formation 语义相近但不同（实习是培训的一种形式）。","C":"• 干扰项 C “学习课程 / 培训”（un parcours d’études）：后文提到 étudier beaucoup de matières（学习很多科目），可推断 formation 在此指“课程/培训”，是正确答案。","D":"• 干扰项 D “游学”（un voyage d’études）：语义相关但过于具体，原文未提及旅行。"}',
    required_answer = true, weight = 1,
    transfer_category = 'FALSE_FRIEND',
    context_level = 'READING',
    construct_code = 'CONTEXT_REPAIR',
    target_word = 'formation',
    display_condition_json = 'null',
    source_reference = 'questionnaire:P4T2-03',
    content_hash = '8f5e4b3bc9a1d97aea4caee29b9c607426a23852896310c14486ae0a0004581e'
WHERE question_bank_id = @bank_id AND question_code = 'P4T2-03' AND version_no = 1 AND deleted = 0;

-- P4T2-04
UPDATE assessment_question q
JOIN assessment_questionnaire_item i ON i.assessment_question_id = q.id
SET q.question_type = 'SINGLE_CHOICE',
    q.stem_text = 'Pendant sa formation, Sophie ______.',
    q.prompt_text = '请阅读上面的完整短文，选择最佳答案。',
    q.options_json = '[{"key":"A","label":"annule souvent des examens"},{"key":"B","label":"réussit toujours tous les examens"},{"key":"C","label":"prépare seulement des examens"},{"key":"D","label":"participe à des examens"}]',
    q.correct_answer_json = '["D"]',
    q.explanation_text = '4. Pendant sa formation, Sophie ______.
题型：细节理解题（假朋友考察）
#题目分析：答案：D
• 干扰项 A “经常取消考试”（annule souvent des examens）：原文未提及取消，annuler 与 passer语义相反。
• 干扰项 B “总是通过所有考试”（réussit toujours tous les examens）：英语 pass an exam = “通过考试”，是强干扰项（负迁移）。但原文只说 doit souvent passer des examens（必须经常参加考试），未提及是否通过。
• 干扰项 C “只准备考试”（prépare seulement des examens）：原文未提及“只准备”，préparer 与 passer 语义不同。
• 干扰项 D “参加考试”（participe à des examens）：法语 passer un examen = “参加考试”，是正确答案。',
    q.score = 1,
    q.section_code = 'P4T2',
    q.required_answer = true,
    q.weight = 1,
    q.transfer_category = 'FALSE_FRIEND',
    q.context_level = 'READING',
    q.construct_code = 'CONTEXT_REPAIR',
    q.target_word = 'passer',
    q.option_explanations_json = '{"A":"• 干扰项 A “经常取消考试”（annule souvent des examens）：原文未提及取消，annuler 与 passer语义相反。","B":"• 干扰项 B “总是通过所有考试”（réussit toujours tous les examens）：英语 pass an exam = “通过考试”，是强干扰项（负迁移）。但原文只说 doit souvent passer des examens（必须经常参加考试），未提及是否通过。","C":"• 干扰项 C “只准备考试”（prépare seulement des examens）：原文未提及“只准备”，préparer 与 passer 语义不同。","D":"• 干扰项 D “参加考试”（participe à des examens）：法语 passer un examen = “参加考试”，是正确答案。"}',
    q.display_condition_json = 'null'
WHERE i.questionnaire_version_id = @version_id AND i.item_code = 'P4T2-04' AND i.deleted = 0 AND q.deleted = 0;
UPDATE assessment_questionnaire_item
SET required_answer = true, scored = true, weight = 1,
    transfer_category = 'FALSE_FRIEND',
    context_level = 'READING',
    construct_code = 'CONTEXT_REPAIR',
    target_word = 'passer',
    option_explanations_json = '{"A":"• 干扰项 A “经常取消考试”（annule souvent des examens）：原文未提及取消，annuler 与 passer语义相反。","B":"• 干扰项 B “总是通过所有考试”（réussit toujours tous les examens）：英语 pass an exam = “通过考试”，是强干扰项（负迁移）。但原文只说 doit souvent passer des examens（必须经常参加考试），未提及是否通过。","C":"• 干扰项 C “只准备考试”（prépare seulement des examens）：原文未提及“只准备”，préparer 与 passer 语义不同。","D":"• 干扰项 D “参加考试”（participe à des examens）：法语 passer un examen = “参加考试”，是正确答案。"}',
    display_condition_json = 'null'
WHERE questionnaire_version_id = @version_id AND item_code = 'P4T2-04' AND deleted = 0;
UPDATE assessment_question_version
SET question_type = 'SINGLE_CHOICE',
    stem_text = 'Pendant sa formation, Sophie ______.',
    prompt_text = '请阅读上面的完整短文，选择最佳答案。',
    options_json = '[{"key":"A","label":"annule souvent des examens"},{"key":"B","label":"réussit toujours tous les examens"},{"key":"C","label":"prépare seulement des examens"},{"key":"D","label":"participe à des examens"}]',
    correct_answer_json = '["D"]',
    explanation_text = '4. Pendant sa formation, Sophie ______.
题型：细节理解题（假朋友考察）
#题目分析：答案：D
• 干扰项 A “经常取消考试”（annule souvent des examens）：原文未提及取消，annuler 与 passer语义相反。
• 干扰项 B “总是通过所有考试”（réussit toujours tous les examens）：英语 pass an exam = “通过考试”，是强干扰项（负迁移）。但原文只说 doit souvent passer des examens（必须经常参加考试），未提及是否通过。
• 干扰项 C “只准备考试”（prépare seulement des examens）：原文未提及“只准备”，préparer 与 passer 语义不同。
• 干扰项 D “参加考试”（participe à des examens）：法语 passer un examen = “参加考试”，是正确答案。',
    option_explanations_json = '{"A":"• 干扰项 A “经常取消考试”（annule souvent des examens）：原文未提及取消，annuler 与 passer语义相反。","B":"• 干扰项 B “总是通过所有考试”（réussit toujours tous les examens）：英语 pass an exam = “通过考试”，是强干扰项（负迁移）。但原文只说 doit souvent passer des examens（必须经常参加考试），未提及是否通过。","C":"• 干扰项 C “只准备考试”（prépare seulement des examens）：原文未提及“只准备”，préparer 与 passer 语义不同。","D":"• 干扰项 D “参加考试”（participe à des examens）：法语 passer un examen = “参加考试”，是正确答案。"}',
    required_answer = true, weight = 1,
    transfer_category = 'FALSE_FRIEND',
    context_level = 'READING',
    construct_code = 'CONTEXT_REPAIR',
    target_word = 'passer',
    display_condition_json = 'null',
    source_reference = 'questionnaire:P4T2-04',
    content_hash = 'e455fed5c4bd372131b14d8c96ad1f0b894def6e0139b56168a1106d604c4187'
WHERE question_bank_id = @bank_id AND question_code = 'P4T2-04' AND version_no = 1 AND deleted = 0;

-- P4T2-05
UPDATE assessment_question q
JOIN assessment_questionnaire_item i ON i.assessment_question_id = q.id
SET q.question_type = 'SINGLE_CHOICE',
    q.stem_text = 'Dans la phrase « elle trouve cette expérience très stimulante », que signifie le mot « stimulante » ?',
    q.prompt_text = '请阅读上面的完整短文，选择最佳答案。',
    q.options_json = '[{"key":"A","label":"fatigante"},{"key":"B","label":"stressante"},{"key":"C","label":"excitante"},{"key":"D","label":"compliquée"}]',
    q.correct_answer_json = '["C"]',
    q.explanation_text = '5. Dans la phrase « elle trouve cette expérience très stimulante », que signifie le mot « stimulante » ?
题型：词汇理解题（同源词考察）
#题目分析：答案：C
• 干扰项 A “令人疲惫的”（fatigante）：语义相反。学习很多科目可能疲劳，但原文语境是积极评价。
• 干扰项 B “压力大的”（stressante）：语义部分相关但不相同，且原文未体现压力。
• 干扰项 C “令人兴奋的 / 振奋的”（excitante）：与 stimulante 同义，且后文提到她希望书店成功，整体语境积极，是正确答案。
• 干扰项 D “复杂的”（compliquée）：语义相关（学习新科目很复杂），但与 stimulante 不同，是弱干扰项。',
    q.score = 1,
    q.section_code = 'P4T2',
    q.required_answer = true,
    q.weight = 1,
    q.transfer_category = 'COGNATE',
    q.context_level = 'READING',
    q.construct_code = 'CONTEXT_REPAIR',
    q.target_word = 'stimulante',
    q.option_explanations_json = '{"A":"• 干扰项 A “令人疲惫的”（fatigante）：语义相反。学习很多科目可能疲劳，但原文语境是积极评价。","B":"• 干扰项 B “压力大的”（stressante）：语义部分相关但不相同，且原文未体现压力。","C":"• 干扰项 C “令人兴奋的 / 振奋的”（excitante）：与 stimulante 同义，且后文提到她希望书店成功，整体语境积极，是正确答案。","D":"• 干扰项 D “复杂的”（compliquée）：语义相关（学习新科目很复杂），但与 stimulante 不同，是弱干扰项。"}',
    q.display_condition_json = 'null'
WHERE i.questionnaire_version_id = @version_id AND i.item_code = 'P4T2-05' AND i.deleted = 0 AND q.deleted = 0;
UPDATE assessment_questionnaire_item
SET required_answer = true, scored = true, weight = 1,
    transfer_category = 'COGNATE',
    context_level = 'READING',
    construct_code = 'CONTEXT_REPAIR',
    target_word = 'stimulante',
    option_explanations_json = '{"A":"• 干扰项 A “令人疲惫的”（fatigante）：语义相反。学习很多科目可能疲劳，但原文语境是积极评价。","B":"• 干扰项 B “压力大的”（stressante）：语义部分相关但不相同，且原文未体现压力。","C":"• 干扰项 C “令人兴奋的 / 振奋的”（excitante）：与 stimulante 同义，且后文提到她希望书店成功，整体语境积极，是正确答案。","D":"• 干扰项 D “复杂的”（compliquée）：语义相关（学习新科目很复杂），但与 stimulante 不同，是弱干扰项。"}',
    display_condition_json = 'null'
WHERE questionnaire_version_id = @version_id AND item_code = 'P4T2-05' AND deleted = 0;
UPDATE assessment_question_version
SET question_type = 'SINGLE_CHOICE',
    stem_text = 'Dans la phrase « elle trouve cette expérience très stimulante », que signifie le mot « stimulante » ?',
    prompt_text = '请阅读上面的完整短文，选择最佳答案。',
    options_json = '[{"key":"A","label":"fatigante"},{"key":"B","label":"stressante"},{"key":"C","label":"excitante"},{"key":"D","label":"compliquée"}]',
    correct_answer_json = '["C"]',
    explanation_text = '5. Dans la phrase « elle trouve cette expérience très stimulante », que signifie le mot « stimulante » ?
题型：词汇理解题（同源词考察）
#题目分析：答案：C
• 干扰项 A “令人疲惫的”（fatigante）：语义相反。学习很多科目可能疲劳，但原文语境是积极评价。
• 干扰项 B “压力大的”（stressante）：语义部分相关但不相同，且原文未体现压力。
• 干扰项 C “令人兴奋的 / 振奋的”（excitante）：与 stimulante 同义，且后文提到她希望书店成功，整体语境积极，是正确答案。
• 干扰项 D “复杂的”（compliquée）：语义相关（学习新科目很复杂），但与 stimulante 不同，是弱干扰项。',
    option_explanations_json = '{"A":"• 干扰项 A “令人疲惫的”（fatigante）：语义相反。学习很多科目可能疲劳，但原文语境是积极评价。","B":"• 干扰项 B “压力大的”（stressante）：语义部分相关但不相同，且原文未体现压力。","C":"• 干扰项 C “令人兴奋的 / 振奋的”（excitante）：与 stimulante 同义，且后文提到她希望书店成功，整体语境积极，是正确答案。","D":"• 干扰项 D “复杂的”（compliquée）：语义相关（学习新科目很复杂），但与 stimulante 不同，是弱干扰项。"}',
    required_answer = true, weight = 1,
    transfer_category = 'COGNATE',
    context_level = 'READING',
    construct_code = 'CONTEXT_REPAIR',
    target_word = 'stimulante',
    display_condition_json = 'null',
    source_reference = 'questionnaire:P4T2-05',
    content_hash = '36289dd635bf64ade201f24288b8fa79f5197dae22663e2435a0190e8bc7de8e'
WHERE question_bank_id = @bank_id AND question_code = 'P4T2-05' AND version_no = 1 AND deleted = 0;

-- P4T3-01
UPDATE assessment_question q
JOIN assessment_questionnaire_item i ON i.assessment_question_id = q.id
SET q.question_type = 'TRUE_FALSE_WITH_JUSTIFICATION',
    q.stem_text = 'Avignon était une destination qui plaisait à beaucoup de monde chaque année.',
    q.prompt_text = '请阅读上面的法语短文，根据短文内容判断下列句子的正误。正确的请写“V”，错误的请写“F”，判断为F的题目需要在横线上写明错误原因。',
    q.options_json = '[{"key":"V","label":"正确"},{"key":"F","label":"错误"}]',
    q.correct_answer_json = '["V"]',
    q.explanation_text = '1. Avignon était une destination qui plaisait à beaucoup de monde chaque année.
题型： 判断正误题（同源词正迁移）
#题目分析：答案：V
• 关键词：attirait（同源词，英语 attract）
• 短文第一句明确写道 “Avignon attirait chaque année des milliers de visiteurs”（阿维尼翁每年吸引成千上万的游客）。题干用 “plaisait à beaucoup de monde” 替换 “attirait des milliers de visiteurs”，语义一致。学生可通过英语 attract 的正迁移理解词义，判断正确。',
    q.score = 1,
    q.section_code = 'P4T3',
    q.required_answer = true,
    q.weight = 1,
    q.transfer_category = 'COGNATE',
    q.context_level = 'READING',
    q.construct_code = 'CONTEXT_REPAIR',
    q.target_word = 'attirait',
    q.option_explanations_json = '{}',
    q.display_condition_json = 'null'
WHERE i.questionnaire_version_id = @version_id AND i.item_code = 'P4T3-01' AND i.deleted = 0 AND q.deleted = 0;
UPDATE assessment_questionnaire_item
SET required_answer = true, scored = true, weight = 1,
    transfer_category = 'COGNATE',
    context_level = 'READING',
    construct_code = 'CONTEXT_REPAIR',
    target_word = 'attirait',
    option_explanations_json = '{}',
    display_condition_json = 'null'
WHERE questionnaire_version_id = @version_id AND item_code = 'P4T3-01' AND deleted = 0;
UPDATE assessment_question_version
SET question_type = 'TRUE_FALSE_WITH_JUSTIFICATION',
    stem_text = 'Avignon était une destination qui plaisait à beaucoup de monde chaque année.',
    prompt_text = '请阅读上面的法语短文，根据短文内容判断下列句子的正误。正确的请写“V”，错误的请写“F”，判断为F的题目需要在横线上写明错误原因。',
    options_json = '[{"key":"V","label":"正确"},{"key":"F","label":"错误"}]',
    correct_answer_json = '["V"]',
    explanation_text = '1. Avignon était une destination qui plaisait à beaucoup de monde chaque année.
题型： 判断正误题（同源词正迁移）
#题目分析：答案：V
• 关键词：attirait（同源词，英语 attract）
• 短文第一句明确写道 “Avignon attirait chaque année des milliers de visiteurs”（阿维尼翁每年吸引成千上万的游客）。题干用 “plaisait à beaucoup de monde” 替换 “attirait des milliers de visiteurs”，语义一致。学生可通过英语 attract 的正迁移理解词义，判断正确。',
    option_explanations_json = '{}',
    required_answer = true, weight = 1,
    transfer_category = 'COGNATE',
    context_level = 'READING',
    construct_code = 'CONTEXT_REPAIR',
    target_word = 'attirait',
    display_condition_json = 'null',
    source_reference = 'questionnaire:P4T3-01',
    content_hash = '8fad53d1cab0501190dd56284ac101c83086f93989ca12ae5b84bfcf0305bebd'
WHERE question_bank_id = @bank_id AND question_code = 'P4T3-01' AND version_no = 1 AND deleted = 0;

-- P4T3-02
UPDATE assessment_question q
JOIN assessment_questionnaire_item i ON i.assessment_question_id = q.id
SET q.question_type = 'TRUE_FALSE_WITH_JUSTIFICATION',
    q.stem_text = 'Léa et son ami avaient déjà commandé leurs places dans le train pour rentrer le soir.',
    q.prompt_text = '请阅读上面的法语短文，根据短文内容判断下列句子的正误。正确的请写“V”，错误的请写“F”，判断为F的题目需要在横线上写明错误原因。',
    q.options_json = '[{"key":"V","label":"正确"},{"key":"F","label":"错误"}]',
    q.correct_answer_json = '["V"]',
    q.explanation_text = '2. Léa et son ami avaient déjà commandé leurs places dans le train pour rentrer le soir.
题型： 判断正误题（同源词正迁移）
#题目分析：答案：V
• 关键词：réservé（同源词，英语 reserve）
• 短文第一段末尾提到 “ils avaient déjà réservé leurs billets de train pour le soir”。题干用 “commandé leurs places” 同义转述 “réservé leurs billets”。英语 reserve 帮助学生理解，判断正确。',
    q.score = 1,
    q.section_code = 'P4T3',
    q.required_answer = true,
    q.weight = 1,
    q.transfer_category = 'COGNATE',
    q.context_level = 'READING',
    q.construct_code = 'CONTEXT_REPAIR',
    q.target_word = 'réservé',
    q.option_explanations_json = '{}',
    q.display_condition_json = 'null'
WHERE i.questionnaire_version_id = @version_id AND i.item_code = 'P4T3-02' AND i.deleted = 0 AND q.deleted = 0;
UPDATE assessment_questionnaire_item
SET required_answer = true, scored = true, weight = 1,
    transfer_category = 'COGNATE',
    context_level = 'READING',
    construct_code = 'CONTEXT_REPAIR',
    target_word = 'réservé',
    option_explanations_json = '{}',
    display_condition_json = 'null'
WHERE questionnaire_version_id = @version_id AND item_code = 'P4T3-02' AND deleted = 0;
UPDATE assessment_question_version
SET question_type = 'TRUE_FALSE_WITH_JUSTIFICATION',
    stem_text = 'Léa et son ami avaient déjà commandé leurs places dans le train pour rentrer le soir.',
    prompt_text = '请阅读上面的法语短文，根据短文内容判断下列句子的正误。正确的请写“V”，错误的请写“F”，判断为F的题目需要在横线上写明错误原因。',
    options_json = '[{"key":"V","label":"正确"},{"key":"F","label":"错误"}]',
    correct_answer_json = '["V"]',
    explanation_text = '2. Léa et son ami avaient déjà commandé leurs places dans le train pour rentrer le soir.
题型： 判断正误题（同源词正迁移）
#题目分析：答案：V
• 关键词：réservé（同源词，英语 reserve）
• 短文第一段末尾提到 “ils avaient déjà réservé leurs billets de train pour le soir”。题干用 “commandé leurs places” 同义转述 “réservé leurs billets”。英语 reserve 帮助学生理解，判断正确。',
    option_explanations_json = '{}',
    required_answer = true, weight = 1,
    transfer_category = 'COGNATE',
    context_level = 'READING',
    construct_code = 'CONTEXT_REPAIR',
    target_word = 'réservé',
    display_condition_json = 'null',
    source_reference = 'questionnaire:P4T3-02',
    content_hash = 'b887f545440ed461cd2245b789dca9cefbd6a5377d07bc7570ec978a00baad04'
WHERE question_bank_id = @bank_id AND question_code = 'P4T3-02' AND version_no = 1 AND deleted = 0;

-- P4T3-03
UPDATE assessment_question q
JOIN assessment_questionnaire_item i ON i.assessment_question_id = q.id
SET q.question_type = 'TRUE_FALSE_WITH_JUSTIFICATION',
    q.stem_text = 'La chaleur était très agréable et rafraîchissante.',
    q.prompt_text = '请阅读上面的法语短文，根据短文内容判断下列句子的正误。正确的请写“V”，错误的请写“F”，判断为F的题目需要在横线上写明错误原因。',
    q.options_json = '[{"key":"V","label":"正确"},{"key":"F","label":"错误"}]',
    q.correct_answer_json = '["F"]',
    q.explanation_text = '3. La chaleur était très agréable et rafraîchissante.
题型： 判断正误题（纯法语词）
#题目分析：答案：F
• 关键词：étouffante（纯法语词，无英语同源）
• 原文明确写道 “La chaleur était étouffante”（天气闷热得令人窒息）。题干却说 “非常舒适凉爽”，与事实完全相反。学生无法借助英语，必须掌握 étouffant 的词义才能判断为错。
错误原因：原文中天气是“闷热窒息”，而非“舒适凉爽”。',
    q.score = 1,
    q.section_code = 'P4T3',
    q.required_answer = true,
    q.weight = 1,
    q.transfer_category = 'FRENCH_CONTROL',
    q.context_level = 'READING',
    q.construct_code = 'CONTEXT_REPAIR',
    q.target_word = 'étouffante',
    q.option_explanations_json = '{}',
    q.display_condition_json = 'null'
WHERE i.questionnaire_version_id = @version_id AND i.item_code = 'P4T3-03' AND i.deleted = 0 AND q.deleted = 0;
UPDATE assessment_questionnaire_item
SET required_answer = true, scored = true, weight = 1,
    transfer_category = 'FRENCH_CONTROL',
    context_level = 'READING',
    construct_code = 'CONTEXT_REPAIR',
    target_word = 'étouffante',
    option_explanations_json = '{}',
    display_condition_json = 'null'
WHERE questionnaire_version_id = @version_id AND item_code = 'P4T3-03' AND deleted = 0;
UPDATE assessment_question_version
SET question_type = 'TRUE_FALSE_WITH_JUSTIFICATION',
    stem_text = 'La chaleur était très agréable et rafraîchissante.',
    prompt_text = '请阅读上面的法语短文，根据短文内容判断下列句子的正误。正确的请写“V”，错误的请写“F”，判断为F的题目需要在横线上写明错误原因。',
    options_json = '[{"key":"V","label":"正确"},{"key":"F","label":"错误"}]',
    correct_answer_json = '["F"]',
    explanation_text = '3. La chaleur était très agréable et rafraîchissante.
题型： 判断正误题（纯法语词）
#题目分析：答案：F
• 关键词：étouffante（纯法语词，无英语同源）
• 原文明确写道 “La chaleur était étouffante”（天气闷热得令人窒息）。题干却说 “非常舒适凉爽”，与事实完全相反。学生无法借助英语，必须掌握 étouffant 的词义才能判断为错。
错误原因：原文中天气是“闷热窒息”，而非“舒适凉爽”。',
    option_explanations_json = '{}',
    required_answer = true, weight = 1,
    transfer_category = 'FRENCH_CONTROL',
    context_level = 'READING',
    construct_code = 'CONTEXT_REPAIR',
    target_word = 'étouffante',
    display_condition_json = 'null',
    source_reference = 'questionnaire:P4T3-03',
    content_hash = '2a617bc5df8abca186cb080834195b4a395d1a10c4fbfa19d1bddb04ed9231f0'
WHERE question_bank_id = @bank_id AND question_code = 'P4T3-03' AND version_no = 1 AND deleted = 0;

-- P4T3-04
UPDATE assessment_question q
JOIN assessment_questionnaire_item i ON i.assessment_question_id = q.id
SET q.question_type = 'TRUE_FALSE_WITH_JUSTIFICATION',
    q.stem_text = 'Les ruelles étaient toutes différentes les unes des autres.',
    q.prompt_text = '请阅读上面的法语短文，根据短文内容判断下列句子的正误。正确的请写“V”，错误的请写“F”，判断为F的题目需要在横线上写明错误原因。',
    q.options_json = '[{"key":"V","label":"正确"},{"key":"F","label":"错误"}]',
    q.correct_answer_json = '["F"]',
    q.explanation_text = '4. Les ruelles étaient toutes différentes les unes des autres.
题型： 判断正误题（纯法语词）
#题目分析：答案：F
• 关键词：pareilles（纯法语词，无英语同源）
• 原文写道 “Les ruelles étaient toutes pareilles”（小巷全都一模一样）。题干说 “各不相同”，与原文相反。
错误原因：原文明确指出小巷“全都一样”，不是“各不相同”。',
    q.score = 1,
    q.section_code = 'P4T3',
    q.required_answer = true,
    q.weight = 1,
    q.transfer_category = 'FRENCH_CONTROL',
    q.context_level = 'READING',
    q.construct_code = 'CONTEXT_REPAIR',
    q.target_word = 'pareilles',
    q.option_explanations_json = '{}',
    q.display_condition_json = 'null'
WHERE i.questionnaire_version_id = @version_id AND i.item_code = 'P4T3-04' AND i.deleted = 0 AND q.deleted = 0;
UPDATE assessment_questionnaire_item
SET required_answer = true, scored = true, weight = 1,
    transfer_category = 'FRENCH_CONTROL',
    context_level = 'READING',
    construct_code = 'CONTEXT_REPAIR',
    target_word = 'pareilles',
    option_explanations_json = '{}',
    display_condition_json = 'null'
WHERE questionnaire_version_id = @version_id AND item_code = 'P4T3-04' AND deleted = 0;
UPDATE assessment_question_version
SET question_type = 'TRUE_FALSE_WITH_JUSTIFICATION',
    stem_text = 'Les ruelles étaient toutes différentes les unes des autres.',
    prompt_text = '请阅读上面的法语短文，根据短文内容判断下列句子的正误。正确的请写“V”，错误的请写“F”，判断为F的题目需要在横线上写明错误原因。',
    options_json = '[{"key":"V","label":"正确"},{"key":"F","label":"错误"}]',
    correct_answer_json = '["F"]',
    explanation_text = '4. Les ruelles étaient toutes différentes les unes des autres.
题型： 判断正误题（纯法语词）
#题目分析：答案：F
• 关键词：pareilles（纯法语词，无英语同源）
• 原文写道 “Les ruelles étaient toutes pareilles”（小巷全都一模一样）。题干说 “各不相同”，与原文相反。
错误原因：原文明确指出小巷“全都一样”，不是“各不相同”。',
    option_explanations_json = '{}',
    required_answer = true, weight = 1,
    transfer_category = 'FRENCH_CONTROL',
    context_level = 'READING',
    construct_code = 'CONTEXT_REPAIR',
    target_word = 'pareilles',
    display_condition_json = 'null',
    source_reference = 'questionnaire:P4T3-04',
    content_hash = '3c906f43ee07a25a452ad9c34ed04e9280554e325763e34c0e0655be988050e3'
WHERE question_bank_id = @bank_id AND question_code = 'P4T3-04' AND version_no = 1 AND deleted = 0;

-- P4T3-05
UPDATE assessment_question q
JOIN assessment_questionnaire_item i ON i.assessment_question_id = q.id
SET q.question_type = 'TRUE_FALSE_WITH_JUSTIFICATION',
    q.stem_text = 'Ils ont utilisé une carte de crédit pour payer, mais la carte était cassée.',
    q.prompt_text = '请阅读上面的法语短文，根据短文内容判断下列句子的正误。正确的请写“V”，错误的请写“F”，判断为F的题目需要在横线上写明错误原因。',
    q.options_json = '[{"key":"V","label":"正确"},{"key":"F","label":"错误"}]',
    q.correct_answer_json = '["F"]',
    q.explanation_text = '5. Ils ont utilisé une carte de crédit pour payer, mais la carte était cassée.
题型： 判断正误题（假朋友负迁移）
#题目分析：答案：F
• 关键词：carte（假朋友，法语 = 地图；英语 card = 信用卡/卡片）
• 原文中的 “La carte ne servait à rien” 指的是地图无用，而不是信用卡。题干故意将 carte曲解为 “carte de crédit”，并添加 “卡坏了” 这一虚构信息，完全不符合原文。受英语 card 干扰的学生可能误以为真。
错误原因：原文中的 carte 是“地图”，并非“信用卡”，且文中无任何支付或卡损坏的情节。',
    q.score = 1,
    q.section_code = 'P4T3',
    q.required_answer = true,
    q.weight = 1,
    q.transfer_category = 'FALSE_FRIEND',
    q.context_level = 'READING',
    q.construct_code = 'CONTEXT_REPAIR',
    q.target_word = 'carte',
    q.option_explanations_json = '{}',
    q.display_condition_json = 'null'
WHERE i.questionnaire_version_id = @version_id AND i.item_code = 'P4T3-05' AND i.deleted = 0 AND q.deleted = 0;
UPDATE assessment_questionnaire_item
SET required_answer = true, scored = true, weight = 1,
    transfer_category = 'FALSE_FRIEND',
    context_level = 'READING',
    construct_code = 'CONTEXT_REPAIR',
    target_word = 'carte',
    option_explanations_json = '{}',
    display_condition_json = 'null'
WHERE questionnaire_version_id = @version_id AND item_code = 'P4T3-05' AND deleted = 0;
UPDATE assessment_question_version
SET question_type = 'TRUE_FALSE_WITH_JUSTIFICATION',
    stem_text = 'Ils ont utilisé une carte de crédit pour payer, mais la carte était cassée.',
    prompt_text = '请阅读上面的法语短文，根据短文内容判断下列句子的正误。正确的请写“V”，错误的请写“F”，判断为F的题目需要在横线上写明错误原因。',
    options_json = '[{"key":"V","label":"正确"},{"key":"F","label":"错误"}]',
    correct_answer_json = '["F"]',
    explanation_text = '5. Ils ont utilisé une carte de crédit pour payer, mais la carte était cassée.
题型： 判断正误题（假朋友负迁移）
#题目分析：答案：F
• 关键词：carte（假朋友，法语 = 地图；英语 card = 信用卡/卡片）
• 原文中的 “La carte ne servait à rien” 指的是地图无用，而不是信用卡。题干故意将 carte曲解为 “carte de crédit”，并添加 “卡坏了” 这一虚构信息，完全不符合原文。受英语 card 干扰的学生可能误以为真。
错误原因：原文中的 carte 是“地图”，并非“信用卡”，且文中无任何支付或卡损坏的情节。',
    option_explanations_json = '{}',
    required_answer = true, weight = 1,
    transfer_category = 'FALSE_FRIEND',
    context_level = 'READING',
    construct_code = 'CONTEXT_REPAIR',
    target_word = 'carte',
    display_condition_json = 'null',
    source_reference = 'questionnaire:P4T3-05',
    content_hash = '69c94ad40506113f413cc51bc5b9264d2767ae47b462793c1629ced25b1df66a'
WHERE question_bank_id = @bank_id AND question_code = 'P4T3-05' AND version_no = 1 AND deleted = 0;

-- P4T3-06
UPDATE assessment_question q
JOIN assessment_questionnaire_item i ON i.assessment_question_id = q.id
SET q.question_type = 'TRUE_FALSE_WITH_JUSTIFICATION',
    q.stem_text = 'L’endroit autour de la fontaine était très petit.',
    q.prompt_text = '请阅读上面的法语短文，根据短文内容判断下列句子的正误。正确的请写“V”，错误的请写“F”，判断为F的题目需要在横线上写明错误原因。',
    q.options_json = '[{"key":"V","label":"正确"},{"key":"F","label":"错误"}]',
    q.correct_answer_json = '["F"]',
    q.explanation_text = '6. L’endroit autour de la fontaine était très petit.
题型： 判断正误题（假朋友负迁移）
#题目分析：答案：F
• 关键词：place（假朋友，法语 = 广场；英语 place = 地方）
• 原文描述喷泉所在的 petite place（小广场）时写道 “Beaucoup de monde se tenait autour”（很多人聚集在周围）。这说明该地方并不小（否则容不下那么多人）。题干说 “非常小” 缺乏依据，且与 “很多人” 的逻辑矛盾。英语 place 泛指“地方”，可能让学生忽略法语特指“广场”且原文人多的事实。
错误原因：原文提到“很多人聚集”，说明地方不小；且文中未提及地方大小，题干判断无依据。',
    q.score = 1,
    q.section_code = 'P4T3',
    q.required_answer = true,
    q.weight = 1,
    q.transfer_category = 'FALSE_FRIEND',
    q.context_level = 'READING',
    q.construct_code = 'CONTEXT_REPAIR',
    q.target_word = 'place',
    q.option_explanations_json = '{}',
    q.display_condition_json = 'null'
WHERE i.questionnaire_version_id = @version_id AND i.item_code = 'P4T3-06' AND i.deleted = 0 AND q.deleted = 0;
UPDATE assessment_questionnaire_item
SET required_answer = true, scored = true, weight = 1,
    transfer_category = 'FALSE_FRIEND',
    context_level = 'READING',
    construct_code = 'CONTEXT_REPAIR',
    target_word = 'place',
    option_explanations_json = '{}',
    display_condition_json = 'null'
WHERE questionnaire_version_id = @version_id AND item_code = 'P4T3-06' AND deleted = 0;
UPDATE assessment_question_version
SET question_type = 'TRUE_FALSE_WITH_JUSTIFICATION',
    stem_text = 'L’endroit autour de la fontaine était très petit.',
    prompt_text = '请阅读上面的法语短文，根据短文内容判断下列句子的正误。正确的请写“V”，错误的请写“F”，判断为F的题目需要在横线上写明错误原因。',
    options_json = '[{"key":"V","label":"正确"},{"key":"F","label":"错误"}]',
    correct_answer_json = '["F"]',
    explanation_text = '6. L’endroit autour de la fontaine était très petit.
题型： 判断正误题（假朋友负迁移）
#题目分析：答案：F
• 关键词：place（假朋友，法语 = 广场；英语 place = 地方）
• 原文描述喷泉所在的 petite place（小广场）时写道 “Beaucoup de monde se tenait autour”（很多人聚集在周围）。这说明该地方并不小（否则容不下那么多人）。题干说 “非常小” 缺乏依据，且与 “很多人” 的逻辑矛盾。英语 place 泛指“地方”，可能让学生忽略法语特指“广场”且原文人多的事实。
错误原因：原文提到“很多人聚集”，说明地方不小；且文中未提及地方大小，题干判断无依据。',
    option_explanations_json = '{}',
    required_answer = true, weight = 1,
    transfer_category = 'FALSE_FRIEND',
    context_level = 'READING',
    construct_code = 'CONTEXT_REPAIR',
    target_word = 'place',
    display_condition_json = 'null',
    source_reference = 'questionnaire:P4T3-06',
    content_hash = '090440ca1f213d6fee66e73b5405a836dfae0876fa74584328787fd61536450d'
WHERE question_bank_id = @bank_id AND question_code = 'P4T3-06' AND version_no = 1 AND deleted = 0;

-- P4T3-07
UPDATE assessment_question q
JOIN assessment_questionnaire_item i ON i.assessment_question_id = q.id
SET q.question_type = 'TRUE_FALSE_WITH_JUSTIFICATION',
    q.stem_text = 'Ils se sont fiancés dans l’église et ont passé un moment romantique.',
    q.prompt_text = '请阅读上面的法语短文，根据短文内容判断下列句子的正误。正确的请写“V”，错误的请写“F”，判断为F的题目需要在横线上写明错误原因。',
    q.options_json = '[{"key":"V","label":"正确"},{"key":"F","label":"错误"}]',
    q.correct_answer_json = '["F"]',
    q.explanation_text = '7. Ils se sont fiancés dans l’église et ont passé un moment romantique.
题型： 判断正误题（假朋友负迁移）
#题目分析：答案：F
• 关键词：engagés（假朋友，法语 = 投入、参与；英语 engaged = 订婚的）
• 原文 “ils se sentaient pleinement engagés dans la contemplation” 意为“他们全身心投入沉思”，与订婚毫无关系。题干利用英语 engaged 的“订婚”义进行浪漫误导。
错误原因：原文中的 engagés 意为“投入（沉思）”，并非“订婚”。',
    q.score = 1,
    q.section_code = 'P4T3',
    q.required_answer = true,
    q.weight = 1,
    q.transfer_category = 'FALSE_FRIEND',
    q.context_level = 'READING',
    q.construct_code = 'CONTEXT_REPAIR',
    q.target_word = 'engagés',
    q.option_explanations_json = '{}',
    q.display_condition_json = 'null'
WHERE i.questionnaire_version_id = @version_id AND i.item_code = 'P4T3-07' AND i.deleted = 0 AND q.deleted = 0;
UPDATE assessment_questionnaire_item
SET required_answer = true, scored = true, weight = 1,
    transfer_category = 'FALSE_FRIEND',
    context_level = 'READING',
    construct_code = 'CONTEXT_REPAIR',
    target_word = 'engagés',
    option_explanations_json = '{}',
    display_condition_json = 'null'
WHERE questionnaire_version_id = @version_id AND item_code = 'P4T3-07' AND deleted = 0;
UPDATE assessment_question_version
SET question_type = 'TRUE_FALSE_WITH_JUSTIFICATION',
    stem_text = 'Ils se sont fiancés dans l’église et ont passé un moment romantique.',
    prompt_text = '请阅读上面的法语短文，根据短文内容判断下列句子的正误。正确的请写“V”，错误的请写“F”，判断为F的题目需要在横线上写明错误原因。',
    options_json = '[{"key":"V","label":"正确"},{"key":"F","label":"错误"}]',
    correct_answer_json = '["F"]',
    explanation_text = '7. Ils se sont fiancés dans l’église et ont passé un moment romantique.
题型： 判断正误题（假朋友负迁移）
#题目分析：答案：F
• 关键词：engagés（假朋友，法语 = 投入、参与；英语 engaged = 订婚的）
• 原文 “ils se sentaient pleinement engagés dans la contemplation” 意为“他们全身心投入沉思”，与订婚毫无关系。题干利用英语 engaged 的“订婚”义进行浪漫误导。
错误原因：原文中的 engagés 意为“投入（沉思）”，并非“订婚”。',
    option_explanations_json = '{}',
    required_answer = true, weight = 1,
    transfer_category = 'FALSE_FRIEND',
    context_level = 'READING',
    construct_code = 'CONTEXT_REPAIR',
    target_word = 'engagés',
    display_condition_json = 'null',
    source_reference = 'questionnaire:P4T3-07',
    content_hash = 'c1d0108d322efbc47021ebf8de530d1e3f3c2d6be9c9e4322e015bd13694f2c8'
WHERE question_bank_id = @bank_id AND question_code = 'P4T3-07' AND version_no = 1 AND deleted = 0;

-- P4T3-08
UPDATE assessment_question q
JOIN assessment_questionnaire_item i ON i.assessment_question_id = q.id
SET q.question_type = 'TRUE_FALSE_WITH_JUSTIFICATION',
    q.stem_text = 'Ils ont trouvé cette expérience difficile et effrayante.',
    q.prompt_text = '请阅读上面的法语短文，根据短文内容判断下列句子的正误。正确的请写“V”，错误的请写“F”，判断为F的题目需要在横线上写明错误原因。',
    q.options_json = '[{"key":"V","label":"正确"},{"key":"F","label":"错误"}]',
    q.correct_answer_json = '["F"]',
    q.explanation_text = '8. Ils ont trouvé cette expérience difficile et effrayante.
题型： 判断正误题（假朋友负迁移）
#题目分析：答案：F
• 关键词：formidable（假朋友，法语 = 绝妙、精彩；英语 formidable = 可怕、棘手）
• 原文 “Tout leur paraissait formidable” 是强烈的积极评价（结合上下文：和平、安静、友善的人）。题干将其译为“艰难又可怕”，这是英语 formidable 的含义，词义方向完全相反。
错误原因：法语 formidable 意为“精彩绝伦”，而题干使用了英语义“可怕棘手”，与原文情感相反。',
    q.score = 1,
    q.section_code = 'P4T3',
    q.required_answer = true,
    q.weight = 1,
    q.transfer_category = 'FALSE_FRIEND',
    q.context_level = 'READING',
    q.construct_code = 'CONTEXT_REPAIR',
    q.target_word = 'formidable',
    q.option_explanations_json = '{}',
    q.display_condition_json = 'null'
WHERE i.questionnaire_version_id = @version_id AND i.item_code = 'P4T3-08' AND i.deleted = 0 AND q.deleted = 0;
UPDATE assessment_questionnaire_item
SET required_answer = true, scored = true, weight = 1,
    transfer_category = 'FALSE_FRIEND',
    context_level = 'READING',
    construct_code = 'CONTEXT_REPAIR',
    target_word = 'formidable',
    option_explanations_json = '{}',
    display_condition_json = 'null'
WHERE questionnaire_version_id = @version_id AND item_code = 'P4T3-08' AND deleted = 0;
UPDATE assessment_question_version
SET question_type = 'TRUE_FALSE_WITH_JUSTIFICATION',
    stem_text = 'Ils ont trouvé cette expérience difficile et effrayante.',
    prompt_text = '请阅读上面的法语短文，根据短文内容判断下列句子的正误。正确的请写“V”，错误的请写“F”，判断为F的题目需要在横线上写明错误原因。',
    options_json = '[{"key":"V","label":"正确"},{"key":"F","label":"错误"}]',
    correct_answer_json = '["F"]',
    explanation_text = '8. Ils ont trouvé cette expérience difficile et effrayante.
题型： 判断正误题（假朋友负迁移）
#题目分析：答案：F
• 关键词：formidable（假朋友，法语 = 绝妙、精彩；英语 formidable = 可怕、棘手）
• 原文 “Tout leur paraissait formidable” 是强烈的积极评价（结合上下文：和平、安静、友善的人）。题干将其译为“艰难又可怕”，这是英语 formidable 的含义，词义方向完全相反。
错误原因：法语 formidable 意为“精彩绝伦”，而题干使用了英语义“可怕棘手”，与原文情感相反。',
    option_explanations_json = '{}',
    required_answer = true, weight = 1,
    transfer_category = 'FALSE_FRIEND',
    context_level = 'READING',
    construct_code = 'CONTEXT_REPAIR',
    target_word = 'formidable',
    display_condition_json = 'null',
    source_reference = 'questionnaire:P4T3-08',
    content_hash = '8487d6a067df9ae6c84012838ec3b39904b75676e66b5dad6588646f6034f0f4'
WHERE question_bank_id = @bank_id AND question_code = 'P4T3-08' AND version_no = 1 AND deleted = 0;

-- P4T3-09
UPDATE assessment_question q
JOIN assessment_questionnaire_item i ON i.assessment_question_id = q.id
SET q.question_type = 'TRUE_FALSE_WITH_JUSTIFICATION',
    q.stem_text = 'Le visage de la Vierge était doux et les a beaucoup touchés.',
    q.prompt_text = '请阅读上面的法语短文，根据短文内容判断下列句子的正误。正确的请写“V”，错误的请写“F”，判断为F的题目需要在横线上写明错误原因。',
    q.options_json = '[{"key":"V","label":"正确"},{"key":"F","label":"错误"}]',
    q.correct_answer_json = '["V"]',
    q.explanation_text = '9. Le visage de la Vierge était doux et les a beaucoup touchés.
题型： 判断正误题（假朋友，本题正确）
#题目分析：答案：V
• 关键词：figure（假朋友，法语 = 脸庞；英语 figure = 身材/数字/人物）
• 原文 “Sa figure douce les a beaucoup touchés” 中，figure 显然指圣母的 脸庞（因为“温柔的脸”）。题干正确转述为“visage”。虽然英语 figure 常指“身材”，但语境（光线落在雕像上、被感动）只能支持法语含义。
判断正确：学生需排除英语干扰，正确理解 figure 在此意为“脸庞”。',
    q.score = 1,
    q.section_code = 'P4T3',
    q.required_answer = true,
    q.weight = 1,
    q.transfer_category = 'FALSE_FRIEND',
    q.context_level = 'READING',
    q.construct_code = 'CONTEXT_REPAIR',
    q.target_word = 'figure',
    q.option_explanations_json = '{}',
    q.display_condition_json = 'null'
WHERE i.questionnaire_version_id = @version_id AND i.item_code = 'P4T3-09' AND i.deleted = 0 AND q.deleted = 0;
UPDATE assessment_questionnaire_item
SET required_answer = true, scored = true, weight = 1,
    transfer_category = 'FALSE_FRIEND',
    context_level = 'READING',
    construct_code = 'CONTEXT_REPAIR',
    target_word = 'figure',
    option_explanations_json = '{}',
    display_condition_json = 'null'
WHERE questionnaire_version_id = @version_id AND item_code = 'P4T3-09' AND deleted = 0;
UPDATE assessment_question_version
SET question_type = 'TRUE_FALSE_WITH_JUSTIFICATION',
    stem_text = 'Le visage de la Vierge était doux et les a beaucoup touchés.',
    prompt_text = '请阅读上面的法语短文，根据短文内容判断下列句子的正误。正确的请写“V”，错误的请写“F”，判断为F的题目需要在横线上写明错误原因。',
    options_json = '[{"key":"V","label":"正确"},{"key":"F","label":"错误"}]',
    correct_answer_json = '["V"]',
    explanation_text = '9. Le visage de la Vierge était doux et les a beaucoup touchés.
题型： 判断正误题（假朋友，本题正确）
#题目分析：答案：V
• 关键词：figure（假朋友，法语 = 脸庞；英语 figure = 身材/数字/人物）
• 原文 “Sa figure douce les a beaucoup touchés” 中，figure 显然指圣母的 脸庞（因为“温柔的脸”）。题干正确转述为“visage”。虽然英语 figure 常指“身材”，但语境（光线落在雕像上、被感动）只能支持法语含义。
判断正确：学生需排除英语干扰，正确理解 figure 在此意为“脸庞”。',
    option_explanations_json = '{}',
    required_answer = true, weight = 1,
    transfer_category = 'FALSE_FRIEND',
    context_level = 'READING',
    construct_code = 'CONTEXT_REPAIR',
    target_word = 'figure',
    display_condition_json = 'null',
    source_reference = 'questionnaire:P4T3-09',
    content_hash = '6308ce70d9bcfc693f5fd1be3422fb184486bac0ba6ee6187c54ecf324bcac5c'
WHERE question_bank_id = @bank_id AND question_code = 'P4T3-09' AND version_no = 1 AND deleted = 0;

-- P4T3-10
UPDATE assessment_question q
JOIN assessment_questionnaire_item i ON i.assessment_question_id = q.id
SET q.question_type = 'TRUE_FALSE_WITH_JUSTIFICATION',
    q.stem_text = 'Ils ont réfléchi profondément en silence dans l’église.',
    q.prompt_text = '请阅读上面的法语短文，根据短文内容判断下列句子的正误。正确的请写“V”，错误的请写“F”，判断为F的题目需要在横线上写明错误原因。',
    q.options_json = '[{"key":"V","label":"正确"},{"key":"F","label":"错误"}]',
    q.correct_answer_json = '["V"]',
    q.explanation_text = '10. Ils ont réfléchi profondément en silence dans l’église.
题型： 判断正误题（同源词正迁移）
#题目分析：答案：V
• 关键词：contemplation（同源词，英语 contemplation）
• 原文 “ils se sentaient pleinement engagés dans la contemplation”，结合上下文（他们长时间安静地待在教堂里，感受和平与沉默），题干 “réfléchi profondément en silence” 是对 contemplation 的合理同义转述。英语 contemplation 帮助学生理解，判断正确。',
    q.score = 1,
    q.section_code = 'P4T3',
    q.required_answer = true,
    q.weight = 1,
    q.transfer_category = 'COGNATE',
    q.context_level = 'READING',
    q.construct_code = 'CONTEXT_REPAIR',
    q.target_word = 'contemplation',
    q.option_explanations_json = '{}',
    q.display_condition_json = 'null'
WHERE i.questionnaire_version_id = @version_id AND i.item_code = 'P4T3-10' AND i.deleted = 0 AND q.deleted = 0;
UPDATE assessment_questionnaire_item
SET required_answer = true, scored = true, weight = 1,
    transfer_category = 'COGNATE',
    context_level = 'READING',
    construct_code = 'CONTEXT_REPAIR',
    target_word = 'contemplation',
    option_explanations_json = '{}',
    display_condition_json = 'null'
WHERE questionnaire_version_id = @version_id AND item_code = 'P4T3-10' AND deleted = 0;
UPDATE assessment_question_version
SET question_type = 'TRUE_FALSE_WITH_JUSTIFICATION',
    stem_text = 'Ils ont réfléchi profondément en silence dans l’église.',
    prompt_text = '请阅读上面的法语短文，根据短文内容判断下列句子的正误。正确的请写“V”，错误的请写“F”，判断为F的题目需要在横线上写明错误原因。',
    options_json = '[{"key":"V","label":"正确"},{"key":"F","label":"错误"}]',
    correct_answer_json = '["V"]',
    explanation_text = '10. Ils ont réfléchi profondément en silence dans l’église.
题型： 判断正误题（同源词正迁移）
#题目分析：答案：V
• 关键词：contemplation（同源词，英语 contemplation）
• 原文 “ils se sentaient pleinement engagés dans la contemplation”，结合上下文（他们长时间安静地待在教堂里，感受和平与沉默），题干 “réfléchi profondément en silence” 是对 contemplation 的合理同义转述。英语 contemplation 帮助学生理解，判断正确。',
    option_explanations_json = '{}',
    required_answer = true, weight = 1,
    transfer_category = 'COGNATE',
    context_level = 'READING',
    construct_code = 'CONTEXT_REPAIR',
    target_word = 'contemplation',
    display_condition_json = 'null',
    source_reference = 'questionnaire:P4T3-10',
    content_hash = '75212e535760d44f23d33638d3df8bb09419e0a5ae4f91eb9578827967dbbe69'
WHERE question_bank_id = @bank_id AND question_code = 'P4T3-10' AND version_no = 1 AND deleted = 0;

-- post-update integrity assertions
SELECT 'sections' AS k, COUNT(*) AS n FROM assessment_questionnaire_section WHERE questionnaire_version_id = @version_id AND deleted = 0
UNION ALL SELECT 'items', COUNT(*) FROM assessment_questionnaire_item WHERE questionnaire_version_id = @version_id AND deleted = 0
UNION ALL SELECT 'scored_items', COUNT(*) FROM assessment_questionnaire_item WHERE questionnaire_version_id = @version_id AND scored = 1 AND deleted = 0
UNION ALL SELECT 'branch_item', COUNT(*) FROM assessment_questionnaire_item WHERE questionnaire_version_id = @version_id AND item_code = 'BASIC-ENGLISH-MAJOR' AND deleted = 0;

COMMIT;
