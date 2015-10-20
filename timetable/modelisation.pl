%(salle, capacité, liste des dates libres[jour, debut, duree])
salle('Amphi A', 100,
  [[1, 8, 180], [1, 28, 120],
  [2, 16, 120],
  [3, 36, 120],
  [4, 4, 240],
  [5, 28, 240]]).
salle('Amphi B', 150, [[1, 4, 240], [2, 4, 240], [3, 4, 240], [4, 4, 240], [5, 4, 240]]).
salle('Amphi C', 50,
  [[1, 28, 120], [1, 44, 120],
  [3, 28, 120], [3, 44, 120],
  [5, 28, 120], [5, 44, 120]]).

%(nom, taille du groupe)
groupe('Master 1 Info', 56).

%(nom, duree, [promotions])
epreuve('identifiant','GRAF', 120, ['Master 1 Info']).
epreuve('GL', 90, ['Master 1 Info']).
epreuve('COMP', 180, ['Master 1 Info']).
epreuve('RESO', 90, ['Master 1 Info']).
epreuve('SAMP', 240, ['Master 1 Info']).

reservation(IdEpreuve, IdSalle, [[Jour, Heure, DureeLibre] | SalleHoraires], NEW_SALLE, Reservation):-
  EPREUVE = SALLE.
  salle(IdSalle, Capacite, _L),
  epreuve(IdEpreuve, Duree, Formation),
  groupe(Formation, Students),
  Students <= Capacite,
  Duree =< DureeLibre.
  salle(IdSalle, Capacite, )
