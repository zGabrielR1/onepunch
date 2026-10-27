package main

import (
	"encoding/json"
	"errors"
	"fmt"
	"math"
	"os"
	"path/filepath"
	"sort"
	"sync"
)

type ExerciseSpec struct {
	ExerciseID  string `json:"exerciseId"`
	Sets        int    `json:"sets"`
	Reps        int    `json:"reps"`
	RestSeconds int    `json:"restSeconds"`
}
type Routine struct {
	ID        string         `json:"id"`
	Name      string         `json:"name"`
	Note      string         `json:"note"`
	UpdatedAt int64          `json:"updatedAt"`
	Archived  bool           `json:"archived"`
	Exercises []ExerciseSpec `json:"exercises"`
}
type Set struct {
	ID         string  `json:"id"`
	ExerciseID string  `json:"exerciseId"`
	Weight     float64 `json:"weight"`
	Reps       int     `json:"reps"`
	Done       bool    `json:"done"`
}
type Workout struct {
	ID        string  `json:"id"`
	RoutineID *string `json:"routineId"`
	Title     string  `json:"title"`
	StartedAt int64   `json:"startedAt"`
	EndedAt   int64   `json:"endedAt"`
	UpdatedAt int64   `json:"updatedAt"`
	Note      string  `json:"note"`
	Sets      []Set   `json:"sets"`
}
type State struct {
	Routines []Routine `json:"routines"`
	Workouts []Workout `json:"workouts"`
	Revision int64     `json:"revision"`
}
type Store struct {
	mu      sync.Mutex
	path    string
	state   State
	catalog map[string]bool
}

func NewStore(path string, catalog map[string]bool) (*Store, error) {
	s := &Store{path: path, catalog: catalog, state: State{Routines: []Routine{}, Workouts: []Workout{}}}
	raw, err := os.ReadFile(path)
	if err == nil {
		err = json.Unmarshal(raw, &s.state)
	} else if errors.Is(err, os.ErrNotExist) {
		err = nil
	}
	if err != nil {
		return nil, fmt.Errorf("read store: %w", err)
	}
	if s.state.Routines == nil {
		s.state.Routines = []Routine{}
	}
	if s.state.Workouts == nil {
		s.state.Workouts = []Workout{}
	}
	return s, nil
}
func (s *Store) validate(in State) error {
	if len(in.Routines) > 500 || len(in.Workouts) > 10000 {
		return errors.New("limite de registros excedido")
	}
	ids := map[string]bool{}
	for _, r := range in.Routines {
		if r.ID == "" || len(r.ID) > 100 || ids["r"+r.ID] || r.Name == "" || len(r.Name) > 240 || r.UpdatedAt < 1 || len(r.Exercises) == 0 || len(r.Exercises) > 50 {
			return errors.New("rotina inválida")
		}
		ids["r"+r.ID] = true
		seen := map[string]bool{}
		for _, e := range r.Exercises {
			if !s.catalog[e.ExerciseID] || seen[e.ExerciseID] || e.Sets < 1 || e.Sets > 20 || e.Reps < 1 || e.Reps > 1000 || e.RestSeconds < 0 || e.RestSeconds > 600 {
				return errors.New("exercício da rotina inválido")
			}
			seen[e.ExerciseID] = true
		}
	}
	for _, w := range in.Workouts {
		if w.ID == "" || len(w.ID) > 100 || ids["w"+w.ID] || w.Title == "" || w.StartedAt < 1 || w.EndedAt < w.StartedAt || w.UpdatedAt < 1 || len(w.Sets) == 0 || len(w.Sets) > 1000 {
			return errors.New("treino inválido")
		}
		ids["w"+w.ID] = true
		seen := map[string]bool{}
		for _, set := range w.Sets {
			if set.ID == "" || seen[set.ID] || !s.catalog[set.ExerciseID] || !set.Done || math.IsNaN(set.Weight) || math.IsInf(set.Weight, 0) || set.Weight < 0 || set.Weight > 2000 || set.Reps < 1 || set.Reps > 1000 {
				return errors.New("série inválida")
			}
			seen[set.ID] = true
		}
	}
	return nil
}
func (s *Store) Sync(in State) (State, error) {
	s.mu.Lock()
	defer s.mu.Unlock()
	if err := s.validate(in); err != nil {
		return State{}, err
	}
	// Own the incoming slices: caller edits must never mutate committed history.
	incoming, err := json.Marshal(in)
	if err != nil {
		return State{}, err
	}
	var owned State
	if err = json.Unmarshal(incoming, &owned); err != nil {
		return State{}, err
	}

	in = owned
	next := State{Revision: s.state.Revision + 1, Routines: []Routine{}, Workouts: []Workout{}}
	routines := map[string]Routine{}
	for _, r := range s.state.Routines {
		routines[r.ID] = r
	}
	for _, r := range in.Routines {
		if prior, ok := routines[r.ID]; !ok || r.UpdatedAt > prior.UpdatedAt {
			routines[r.ID] = r
		}
	}
	workouts := map[string]Workout{}
	for _, w := range s.state.Workouts {
		workouts[w.ID] = w
	}
	for _, w := range in.Workouts {
		if prior, ok := workouts[w.ID]; ok {
			a, _ := json.Marshal(prior)
			b, _ := json.Marshal(w)
			if string(a) != string(b) {
				return State{}, errors.New("treino já sincronizado com conteúdo diferente")
			}
		} else {
			workouts[w.ID] = w
		}
	}
	for _, r := range routines {
		next.Routines = append(next.Routines, r)
	}
	for _, w := range workouts {
		next.Workouts = append(next.Workouts, w)
	}
	sort.Slice(next.Routines, func(i, j int) bool { return next.Routines[i].ID < next.Routines[j].ID })
	sort.Slice(next.Workouts, func(i, j int) bool { return next.Workouts[i].EndedAt > next.Workouts[j].EndedAt })
	if len(next.Routines) > 500 || len(next.Workouts) > 10000 {
		return State{}, errors.New("limite de registros excedido")
	}
	raw, err := json.MarshalIndent(next, "", "  ")
	if err != nil {
		return State{}, err
	}
	if err = os.MkdirAll(filepath.Dir(s.path), 0700); err != nil {
		return State{}, err
	}
	f, err := os.CreateTemp(filepath.Dir(s.path), ".state-*")
	if err != nil {
		return State{}, err
	}
	temp := f.Name()
	defer os.Remove(temp)
	if _, err = f.Write(raw); err != nil {
		f.Close()
		return State{}, err
	}
	if err = f.Sync(); err != nil {
		f.Close()
		return State{}, err
	}
	if err = f.Close(); err != nil {
		return State{}, err
	}
	if err = os.Rename(temp, s.path); err != nil {
		return State{}, err
	}
	s.state = next
	var snapshot State
	if err = json.Unmarshal(raw, &snapshot); err != nil {
		return State{}, err
	}
	return snapshot, nil
}
