package main

import (
	"net/http"
	"net/http/httptest"
	"os"
	"path/filepath"
	"strings"
	"sync"
	"testing"
)

func TestDurableMergeAndConflict(t *testing.T) {
	path := filepath.Join(t.TempDir(), "state.json")
	s, err := NewStore(path, map[string]bool{"squat": true})
	if err != nil {
		t.Fatal(err)
	}
	r := Routine{ID: "r", Name: "Pernas", UpdatedAt: 1, Exercises: []ExerciseSpec{{ExerciseID: "squat", Sets: 3, Reps: 10, RestSeconds: 90}}}
	w := Workout{ID: "w", Title: "Treino", StartedAt: 1, EndedAt: 2, UpdatedAt: 2, Sets: []Set{{ID: "s", ExerciseID: "squat", Weight: 20, Reps: 10, Done: true}}}
	input := State{Routines: []Routine{r}, Workouts: []Workout{w}}
	if _, err = s.Sync(input); err != nil {
		t.Fatal(err)
	}
	r.Name = "Novo"
	r.UpdatedAt = 3
	input.Routines = []Routine{r}
	if _, err = s.Sync(input); err != nil {
		t.Fatal(err)
	}
	reopened, err := NewStore(path, s.catalog)
	if err != nil {
		t.Fatal(err)
	}
	if reopened.state.Routines[0].Name != "Novo" || len(reopened.state.Workouts) != 1 {
		t.Fatal("not durable/idempotent")
	}
	input.Workouts[0].Sets[0].Weight = 99
	if _, err = s.Sync(input); err == nil {
		t.Fatal("history overwritten")
	}
}
func TestInvalidRequestDoesNotChangeState(t *testing.T) {
	s, _ := NewStore(filepath.Join(t.TempDir(), "s.json"), map[string]bool{"squat": true})
	h := handler(s, []byte("[]"), "")
	for _, body := range []string{`{"routines":[{"id":"r","name":"bad","updatedAt":1,"exercises":[]}]}`, `{"unknown":true}`, `{} {}`} {
		r := httptest.NewRequest(http.MethodPost, "/api/sync", strings.NewReader(body))
		r.Header.Set("Content-Type", "application/json")
		w := httptest.NewRecorder()
		h.ServeHTTP(w, r)
		if w.Code < 400 {
			t.Fatalf("accepted %s", body)
		}
	}
	if s.state.Revision != 0 {
		t.Fatal("invalid write changed state")
	}
}
func TestConcurrentDistinctWorkoutsSurvive(t *testing.T) {
	s, _ := NewStore(filepath.Join(t.TempDir(), "s.json"), map[string]bool{"squat": true})
	var wg sync.WaitGroup
	for _, id := range []string{"a", "b", "c"} {
		wg.Add(1)
		go func(id string) {
			defer wg.Done()
			_, err := s.Sync(State{Workouts: []Workout{{ID: id, Title: id, StartedAt: 1, EndedAt: 2, UpdatedAt: 2, Sets: []Set{{ID: id, ExerciseID: "squat", Weight: 0, Reps: 5, Done: true}}}}})
			if err != nil {
				t.Error(err)
			}
		}(id)
	}
	wg.Wait()
	if len(s.state.Workouts) != 3 {
		t.Fatal("lost concurrent workout")
	}
}

func TestStaticAndAPIRoutingCoexist(t *testing.T) {
	root := t.TempDir()
	os.WriteFile(filepath.Join(root, "index.html"), []byte("onepunch app"), 0600)
	s, _ := NewStore(filepath.Join(t.TempDir(), "state.json"), map[string]bool{})
	h := handler(s, []byte("[]"), root)
	for _, path := range []string{"/", "/api/health", "/history"} {
		w := httptest.NewRecorder()
		h.ServeHTTP(w, httptest.NewRequest("GET", path, nil))
		if w.Code != 200 {
			t.Fatalf("%s: %d", path, w.Code)
		}
	}
}
