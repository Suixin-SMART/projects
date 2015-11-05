:- use_module(library(clpfd)).

%(+salle, -capacité, -liste des dates libres[jour, debut, duree])
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

%(+nom, -taille du groupe)
groupe('Master 1 Info', 56).
groupe('Master 2 Info CODES', 10)
groupe('Master 2 Info SDR', 15)

%(+nom, -duree, -[promotions])
epreuve('GRAF', 120, ['Master 1 Info']).
epreuve('GL', 90, ['Master 1 Info']).
epreuve('COMP', 180, ['Master 1 Info']).
epreuve('RESO', 90, ['Master 1 Info']).
epreuve('SAMP', 240, ['Master 1 Info']).
epreuve('AD', 240, ['Master 2 Info CODES','Master 2 Info SDR']).
epreuve('Test', 240, ['Master 2 Info CODES','Master 2 Info SDR']).
epreuve('OC', 240, ['Master 2 Info CODES','Master 2 Info SDR']).
epreuve('IDM', 240, ['Master 2 Info CODES']).
epreuve('PAD', 240, ['Master 2 Info SDR']).

%reservation(IdEpreuve, IdSalle, [[Jour, Heure, DureeLibre] | SalleHoraires], NEW_SALLE, Reservation):-
%  EPREUVE = SALLE.
%  salle(IdSalle, Capacite, _L),
%  epreuve(IdEpreuve, Duree, Formation),
%  groupe(Formation, Students),
%  Students <= Capacite,
%  Duree =< DureeLibre.
%  salle(IdSalle, Capacite, )

% optimizeTimeTable (-ListeSalleLibre, -ListeExamens, -ListeExamensCompatibles, -DeltaMinEntreExamens, +Resultat).
% ListeSalleLibre = [[NomSalle, Capacité, [[Jour, HeureDebutQuartHeure, DureeMinute] | ResteHoraires]] | ResteSalles]
% ListeExamens = [[IdentifiantEpreuve, DureeEpreuve, NombreEtudiants, [Filière | ResteFilière]] | ResteExamens]
% ListeExamensCompatibles = [EnsembleCompatible | ResteEnsembleCompatible]
% DeltaMinEntreExamens = entier (durée en minutes par exemple)
% Resultat =

nbEtudiants([],0).

nbEtudiants([Groupe|ResteGroupe],Rez):-
  groupe(Groupe,nb),
  nbEtudiants(ResteGroupe,NewRez),
  Rez = nb + NewRez.

getDispoDuree([Jour, Debut, Duree], RezDuree):-
  RezDuree = Duree.

getHoraire(Duree, [], 0, []):-

getHoraire(Duree, [FirstDispo|ResteDispo], Rez, NewDispo):-
  getDispoDuree(FirstDispo, Dispo),
  Dispo < Duree,
  getHoraire(Duree, ResteDispo, Rez, NewDispo).

getHoraire(Duree, [FirstDispo|ResteDispo], Rez, NewDispo):-
  getDispoDuree(FirstDispo, Dispo),
  Dispo >= Duree,
  Rez = FirstDispo,
  NewDispo = ResteDispo.


comparer(Examen, Amphi, Rez):-
  epreuve(Examen, EpreuveMinutes, ListeEtudiants),
  nbEtudiants(ListeEtudiants, NbEtudiants),
  salle(Amphi, Capacite, ListeDisponibles),
  NbEtudiants >= Capacite,
  getHoraire(EpreuveMinutes, ListeDisponibles, Horaire, NewListeDisponibles),
  ListeDisponibles = NewListeDisponibles

%(+listeEpreuves, +listeSalles, -listeResultats)
schedule([Examen|ResteExamen],[Amphi|ResteAmphis], Resultat):-
  comparer(Examen, Amphi, Rez).



%addContraints(Param, S0, S):-
%  gen_state(S0),
%  constraint().

%optimizeTimeTable():-
%  addContraints(State0),
%  length(PropositionAffectation, NbAffectations),
%  minimize(NbAffectations, State0, StateNew).

%  knapsack_constrain(S) :-
%        gen_state(S0),
%        constraint([6*x(1), 4*x(2)] =< 8, S0, S1),
%        constraint([x(1)] =< 1, S1, S2),
%        constraint([x(2)] =< 2, S2, S).
%
%knapsack(S) :-
%        knapsack_constrain(S0),
%        maximize([7*x(1), 4*x(2)], S0, S).
%
