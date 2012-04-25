(ns twpsong.sound
  (:use
   [overtone.core]
  )
)

(def notes [60 62 64 65 67 69 71 72])

;; many of these sounds were pilfered from the internet, various authors
;; or the overtone examples themselves

;(definst twpkick [freq 120 dur 0.3 width 0.5]
;  (let [freq-env (* freq (env-gen (perc 0 (* 0.99 dur))))
;        env (env-gen (perc 0.01 dur) 1 1 0 1 FREE)
;        sqr (* (env-gen (perc 0 0.01)) (pulse (* 2 freq) width))
;        src (sin-osc freq-env)
;        drum (+ sqr (* env src))]
;    (compander drum drum 0.2 1 0.1 0.01 0.01)))

(defcgen twpkick
  "basic synthesised kick drum"
  [bpm {:default 120 :doc "tempo of kick in beats per minute"}
   pattern {:default [1 0] :doc "sequence pattern of beats"}]
  (:ar
   (let [kickenv (decay (t2a (demand (impulse:kr (/ bpm 30)) 0 (dseq pattern INF))) 0.7)
         kick (* (* kickenv 7) (sin-osc (+ 40 (* kickenv kickenv kickenv 200))))]
     (clip2 kick 1))))

(defsynth dubstep [bpm 120 wobble 1 note 50 snare-vol 1 kick-vol 1 v 1]
 (let [trig (impulse:kr (/ bpm 120))
       freq (midicps note)
       swr (demand trig 0 (dseq [wobble] INF))
       sweep (lin-exp (lf-tri swr) -1 1 40 3000)
       wob (apply + (saw (* freq [0.99 1.01])))
       wob (lpf wob sweep)
       wob (* 0.8 (normalizer wob))
       wob (+ wob (bpf wob 1500 2))
       wob (+ wob (* 0.2 (g-verb wob 9 0.7 0.7)))

       ;kickenv (decay (t2a (demand (impulse:kr (/ bpm 30)) 0 (dseq [1 0 0 0 0 0 1 0 1 0 0 1 0 0 0 0] INF))) 0.7)
       ;kick (* (* kickenv 7) (sin-osc (+ 40 (* kickenv kickenv kickenv 200))))
       ;kick (clip2 kick 1)

       ;snare (* 3 (pink-noise [1 1]) (apply + (* (decay (impulse (/ bpm 240) 0.5) [0.4 2]) [1 0.05])))
       ;snare (+ snare (bpf (* 4 snare) 2000))
       ;snare (clip2 snare 1)
	]

   (out 0    (* v (clip2 (+ wob 1) 1)))))

(defn play-notes
  [t speed instrument notes]
  (let [n (first notes)
        notes (next notes)
        t-next (+ t speed)]
    (when n
      (at t
          (instrument (note n)))
      (apply-at t-next #'play-notes [t-next speed instrument notes]))))
