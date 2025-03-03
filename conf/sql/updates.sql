update assessment_method
set source = 'rpo'
where id in (
             'written-exam',
             'written-exam-answer-choice-method',
             'oral-exam',
             'home-assignment',
             'open-book-exam',
             'project',
             'portfolio',
             'practical-report',
             'oral-contribution',
             'certificate-achievement',
             'performance-assessment',
             'role-play',
             'admission-colloquium',
             'specimen'
    );
