-- HackMatch database schema
-- All hackathon rows in this file are FICTIONAL demo data for the college project.
-- This script creates missing objects only. It does not drop the database or tables.

CREATE DATABASE IF NOT EXISTS hackmatch
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE hackmatch;

CREATE TABLE IF NOT EXISTS hackathons (
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    name VARCHAR(150) NOT NULL,
    organizer VARCHAR(150) NOT NULL,
    domain VARCHAR(80) NOT NULL,
    prize DECIMAL(12, 2) UNSIGNED NOT NULL DEFAULT 0,
    deadline DATE NOT NULL,
    mode ENUM('Online', 'Offline', 'Hybrid') NOT NULL,
    min_team_size TINYINT UNSIGNED NOT NULL,
    max_team_size TINYINT UNSIGNED NOT NULL,
    required_skills TEXT NOT NULL,
    problem_statement TEXT NOT NULL,
    eligibility TEXT NOT NULL,
    registration_url VARCHAR(500) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY unique_hackathon_name (name),
    CONSTRAINT check_team_size_range CHECK (min_team_size >= 1 AND max_team_size >= min_team_size),
    CONSTRAINT check_prize_non_negative CHECK (prize >= 0)
);

CREATE TABLE IF NOT EXISTS student_profiles (
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    skills TEXT NOT NULL,
    interests TEXT NOT NULL,
    experience_level ENUM('Beginner', 'Intermediate', 'Advanced') NOT NULL DEFAULT 'Beginner',
    preferred_min_prize DECIMAL(12, 2) UNSIGNED NOT NULL DEFAULT 0,
    team_size TINYINT UNSIGNED NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT check_profile_team_size CHECK (team_size >= 1),
    CONSTRAINT check_profile_prize_non_negative CHECK (preferred_min_prize >= 0)
);

CREATE TABLE IF NOT EXISTS bookmarks (
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    student_id INT UNSIGNED NOT NULL,
    hackathon_id INT UNSIGNED NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY unique_student_hackathon_bookmark (student_id, hackathon_id),
    CONSTRAINT fk_bookmarks_student
        FOREIGN KEY (student_id) REFERENCES student_profiles(id) ON DELETE CASCADE,
    CONSTRAINT fk_bookmarks_hackathon
        FOREIGN KEY (hackathon_id) REFERENCES hackathons(id) ON DELETE CASCADE
);

-- INSERT IGNORE makes the named demo hackathons safe to seed repeatedly.
INSERT IGNORE INTO hackathons
    (name, organizer, domain, prize, deadline, mode, min_team_size, max_team_size,
     required_skills, problem_statement, eligibility, registration_url)
VALUES
    ('CodeSprint AI Challenge', 'Nova Tech Society (Fictional Demo)', 'AI/ML', 150000, '2026-11-20', 'Online', 2, 4,
     'Python, Machine Learning, Data Analysis',
     'Design a machine learning system that predicts a student''s weak subjects from past test scores and suggests a personalised weekly study plan.',
     'Undergraduate students from any college in India.', 'https://example.com/register/codesprint-ai'),
    ('WebCraft 2026', 'Pixel Developers Club (Fictional Demo)', 'Web Development', 75000, '2026-12-05', 'Offline', 1, 3,
     'HTML/CSS, JavaScript, UI/UX Design',
     'Build a responsive, accessible website that lets a small non-profit list volunteer events and accept volunteer sign-ups.',
     'Students in 1st to 4th year of any degree. Fictional venue: Pune.', 'https://example.com/register/webcraft'),
    ('FinFuture Hack', 'Ledger Labs Foundation (Fictional Demo)', 'FinTech', 200000, '2026-11-15', 'Online', 2, 5,
     'Java, SQL, JavaScript',
     'Build an expense tracker that categorises transactions automatically and shows simple monthly savings insights.',
     'Students and recent graduates within one year in India.', 'https://example.com/register/finfuture'),
    ('MediHack India', 'CareBridge Health Trust (Fictional Demo)', 'Healthcare', 100000, '2027-01-10', 'Offline', 3, 4,
     'Python, Java, Data Analysis',
     'Create a lightweight appointment and patient-record system that works on low-end devices for rural clinics.',
     'Engineering and medical students. Fictional venue: Bengaluru.', 'https://example.com/register/medihack'),
    ('OpenIdeas Hackfest', 'Campus Innovators Network (Fictional Demo)', 'Open Innovation', 50000, '2026-11-30', 'Online', 1, 4,
     'Java, JavaScript, UI/UX Design',
     'Identify one everyday problem on your college campus and build a working prototype that solves it.',
     'Any college student in India. Beginners are encouraged.', 'https://example.com/register/openideas'),
    ('DeepVision Datathon', 'Insight AI Collective (Fictional Demo)', 'AI/ML', 120000, '2026-12-18', 'Offline', 2, 3,
     'Python, Machine Learning, SQL',
     'Train a model that detects common crop diseases from leaf images and explain its predictions to farmers.',
     'Undergraduate and postgraduate students. Fictional venue: Hyderabad.', 'https://example.com/register/deepvision');

-- This profile makes bookmark API demonstrations possible. It is not an account system.
INSERT INTO student_profiles
    (name, skills, interests, experience_level, preferred_min_prize, team_size)
SELECT 'Demo Student', 'Java, SQL, JavaScript', 'FinTech, Open Innovation', 'Intermediate', 50000, 3
WHERE NOT EXISTS (
    SELECT 1 FROM student_profiles WHERE name = 'Demo Student'
);
