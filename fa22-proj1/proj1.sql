-- Before running drop any existing views
DROP VIEW IF EXISTS q0;
DROP VIEW IF EXISTS q1i;
DROP VIEW IF EXISTS q1ii;
DROP VIEW IF EXISTS q1iii;
DROP VIEW IF EXISTS q1iv;
DROP VIEW IF EXISTS q2i;
DROP VIEW IF EXISTS q2ii;
DROP VIEW IF EXISTS q2iii;
DROP VIEW IF EXISTS q3i;
DROP VIEW IF EXISTS q3ii;
DROP VIEW IF EXISTS q3iii;
DROP VIEW IF EXISTS q4i;
DROP VIEW IF EXISTS q4ii;
DROP VIEW IF EXISTS q4iii;
DROP VIEW IF EXISTS q4iv;
DROP VIEW IF EXISTS q4v;
DROP VIEW IF EXISTS CAcollege;
DROP VIEW IF EXISTS slg;
DROP VIEW IF EXISTS lslg;
DROP VIEW IF EXISTS salary_statistics;
DROP VIEW IF EXISTS maxid;
DROP VIEW IF EXISTS bins_statistics;
DROP VIEW IF EXISTS bins;

-- Question 0
CREATE VIEW q0(era)
AS
  SELECT MAX(era)
  FROM pitching
;

-- Question 1i
CREATE VIEW q1i(namefirst, namelast, birthyear)
AS
  SELECT namefirst,namelast,birthyear
  FROM people
  where weight >300
;

-- Question 1ii
CREATE VIEW q1ii(namefirst, namelast, birthyear)
AS
  SELECT namefirst,namelast,birthyear
  FROM people
  where namefirst like '% %'
  order by namefirst,namelast
;
-- Question 1iii
CREATE VIEW q1iii(birthyear, avgheight, count)
AS
  SELECT birthyear,avg(height),count(*)
  FROM people
  group by birthyear
  order by birthyear
;

-- Question 1iv
CREATE VIEW q1iv(birthyear, avgheight, count)
AS
  SELECT birthyear,avg(height),count(*)
  FROM people
  group by birthyear
  having avg(height) >70
  order by birthyear
;

-- Question 2i
CREATE VIEW q2i(namefirst, namelast, playerid, yearid)
AS
  SELECT namefirst,namelast,playerID,yearID
  from people
  NATURAL JOIN halloffame
  where inducted = 'Y'
  order by yearID desc,playerID
;

-- Question 2ii
-- CREATE VIEW CAcollege(playerid, schoolid)
-- AS 
  
-- ;

CREATE VIEW q2ii(namefirst, namelast, playerid, schoolid, yearid)
AS
  SELECT namefirst,namelast,p.playerID,s.schoolID,h.yearID
  from people p
  NATURAL JOIN halloffame h
  JOIN collegeplaying c
  ON p.playerID = c.playerID
  JOIN schools s
  ON s.schoolID = c.schoolID
  where inducted = 'Y' and schoolState = 'CA'
  order by h.yearID desc,p.playerID
;

-- Question 2iii
CREATE VIEW q2iii(playerid, namefirst, namelast, schoolid)
AS
  SELECT p.playerID,namefirst,namelast,schoolid
  from people p
  NATURAL JOIN halloffame h
  LEFT JOIN collegeplaying c
  ON  p.playerID = c.playerID
  where inducted = 'Y'
  order by p.playerID desc,schoolID
;

-- Question 3i
CREATE VIEW slg(playerid, yearid, AB, slgval)
AS 
  SELECT playerid, yearid, AB, (H + H2B + 2*H3B + 3*HR + 0.0)/(AB + 0.0)
  FROM batting
;

CREATE VIEW q3i(playerid, namefirst, namelast, yearid, slg)
AS
  SELECT p.playerid, p.namefirst, p.namelast, s.yearid, s.slgval
  FROM people p INNER JOIN slg s
  ON p.playerid = s.playerid
  WHERE s.AB > 50
  ORDER BY s.slgval DESC, s.yearid, p.playerid
  LIMIT 10
;

-- Question 3ii
CREATE VIEW lslg(playerid, lslgval)
AS 
;

CREATE VIEW q3ii(playerid, namefirst, namelast, lslg)
AS
;

-- Question 3iii
CREATE VIEW q3iii(namefirst, namelast, lslg)
AS
;

-- Question 4i
CREATE VIEW q4i(yearid, min, max, avg)
AS
;


-- Helper table for 4ii
DROP TABLE IF EXISTS binids;
CREATE TABLE binids(binid);
INSERT INTO binids VALUES (0), (1), (2), (3), (4), (5), (6), (7), (8), (9);

-- Question 4ii
CREATE VIEW bins_statistics(binstart, binend, width)
AS 
;

CREATE VIEW bins(binid, binstart, width)
AS 
;

CREATE VIEW q4ii(binid, low, high, count)
AS
;

-- Question 4iii
CREATE VIEW salary_statistics(yearid, minsa, maxsa, avgsa)
AS 
;

CREATE VIEW q4iii(yearid, mindiff, maxdiff, avgdiff)
AS
;

-- Question 4iv
CREATE VIEW maxid(playerid, salary, yearid)
AS
;

CREATE VIEW q4iv(playerid, namefirst, namelast, salary, yearid)
AS
;
-- Question 4v
CREATE VIEW q4v(team, diffAvg) AS
;

