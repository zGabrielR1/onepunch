package main

import (
	"encoding/json"
	"flag"
	"io"
	"log"
	"net/http"
	"os"
	"path/filepath"
	"strings"
	"time"
)

func handler(store *Store, catalog []byte, web string) http.Handler {
	mux := http.NewServeMux()
	reply := func(w http.ResponseWriter, status int, v any) {
		w.Header().Set("Content-Type", "application/json; charset=utf-8")
		w.Header().Set("Cache-Control", "no-store")
		w.WriteHeader(status)
		json.NewEncoder(w).Encode(v)
	}
	mux.HandleFunc("GET /api/health", func(w http.ResponseWriter, r *http.Request) {
		reply(w, 200, map[string]any{"ok": true, "service": "onepunch", "version": 1})
	})
	mux.HandleFunc("GET /api/exercises", func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json; charset=utf-8")
		w.Write(catalog)
	})
	mux.HandleFunc("POST /api/sync", func(w http.ResponseWriter, r *http.Request) {
		if !strings.HasPrefix(r.Header.Get("Content-Type"), "application/json") {
			reply(w, 415, map[string]string{"error": "Envie JSON."})
			return
		}
		r.Body = http.MaxBytesReader(w, r.Body, 8<<20)
		defer r.Body.Close()
		dec := json.NewDecoder(r.Body)
		dec.DisallowUnknownFields()
		var state State
		if err := dec.Decode(&state); err != nil {
			reply(w, 400, map[string]string{"error": "Dados inválidos."})
			return
		}
		var extra any
		if dec.Decode(&extra) != io.EOF {
			reply(w, 400, map[string]string{"error": "Envie apenas um documento JSON."})
			return
		}
		next, err := store.Sync(state)
		if err != nil {
			reply(w, 409, map[string]string{"error": "Não foi possível sincronizar. " + err.Error()})
			return
		}
		reply(w, 200, next)
	})
	mux.HandleFunc("/api/", func(w http.ResponseWriter, r *http.Request) {
		reply(w, 404, map[string]string{"error": "Rota não encontrada."})
	})
	if web != "" {
		files := http.FileServer(http.Dir(web))
		mux.HandleFunc("/", func(w http.ResponseWriter, r *http.Request) {
			if r.Method != http.MethodGet && r.Method != http.MethodHead {
				http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
				return
			}
			if _, err := os.Stat(filepath.Join(web, filepath.Clean("/"+r.URL.Path))); err != nil && !strings.Contains(filepath.Base(r.URL.Path), ".") {
				http.ServeFile(w, r, filepath.Join(web, "index.html"))
				return
			}
			files.ServeHTTP(w, r)
		})
	}
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("X-Content-Type-Options", "nosniff")
		mux.ServeHTTP(w, r)
	})
}
func main() {
	addr := flag.String("addr", "127.0.0.1:8097", "listen address; keep loopback for this personal prototype")
	data := flag.String("data", "data/state.json", "durable data file")
	catalogPath := flag.String("catalog", "../shared/catalog.json", "shared exercise catalog")
	web := flag.String("web", "../web/dist", "built React client directory")
	flag.Parse()
	catalog, err := os.ReadFile(*catalogPath)
	if err != nil {
		log.Fatal(err)
	}
	var exercises []struct {
		ID string `json:"id"`
	}
	if err = json.Unmarshal(catalog, &exercises); err != nil {
		log.Fatal(err)
	}
	known := map[string]bool{}
	for _, e := range exercises {
		known[e.ID] = true
	}
	store, err := NewStore(*data, known)
	if err != nil {
		log.Fatal(err)
	}
	server := &http.Server{Addr: *addr, Handler: handler(store, catalog, *web), ReadHeaderTimeout: 5 * time.Second, ReadTimeout: 15 * time.Second, WriteTimeout: 15 * time.Second, IdleTimeout: 60 * time.Second}
	log.Printf("Onepunch: http://%s", *addr)
	log.Fatal(server.ListenAndServe())
}
