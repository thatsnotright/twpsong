(ns twpsong.core
  (:use
   [overtone.core]
   [twpsong.sound :as twp]
   [twpsong.feeling]
   [twitter.oauth]
   [twitter.callbacks]
   [twitter.callbacks.handlers]
   [twitter.api.restful]
   [twitter.api.streaming]
  )
  (:require
   [clojure.data.json :as json]
   [http.async.client :as ac]
  )
  (:import
   (twitter.callbacks.protocols AsyncStreamingCallback)
   (twitter.callbacks.protocols SyncSingleCallback)
   (java.util Date)
   )
)


(use 'overtone.inst.piano)

(def ^:dynamic pianodelay 320)

(def ^:dynamic drumbpm 120)

(defn callpiano
  "play some piano notes based on a tweet"
  [content]
  (def tweet (str content)); (:text (json/read-json (str content))))
  (def strcnt (count tweet))
;  (if (> strcnt 30) (if (< strcnt 35) (twp/twpkick) ))
   (play-notes (now) pianodelay piano
        (map
                #(nth notes %)
                (map
                        #(int
                                (/
                                        (*
                                                (- (int %) 32)
                                         8)
                                 94)
                         )
                  (seq tweet)
                )
        )
   )
)

(def ^:dynamic *dubstepid* (:target (dubstep)))

(defn updatedub
   "update params for dubstep background"
   [params]

   (def strcnt (count (params "tweet")))
   (def cwobblerate 280) ;((node-get-control *dubstepid*) :wobble))
   (def wobrate
        (if
                (= (params "emotion") :happy) (- cwobblerate strcnt) (+ cwobblerate strcnt)
        )
   )
;   (if (< 3 wobrate) (def wrate 3) (if (> 50 wobrate) (def wrate 50) ))
(def wrate 8)
   (ctl *dubstepid* :wobble wrate)
)

(defmulti play-sound
  (fn[x] (x "inst")))

(defmethod play-sound 3 [params];
  (if (= (params "emotion") :happy)
    (doseq [n [96 100 103]] (piano n))
    (doseq [n [55 59 64]] (piano n))
  )
  )

;; play a piano melody from the text mapped to an octave
(defmethod play-sound 0 [params]
  (callpiano (params "tweet"))
  )

(defmethod play-sound 1 [params]
        (twp/round-kick)
)

(defmethod play-sound 2 [params]
        (twp/rise-fall-pad)
)

(defmethod play-sound :default [params]
        (updatedub params)
)

(def prevt 0)
; last time we played a piano melody
(def last_piano 0)
; last time we played a base drum
(def last_kick 0)

; current bpm derived from average tweet length normalized
; 140 bytes maps to 100-160 BPM
(def bpm 142)

(defn decided-to-play [tweet]
;  (def instselect (rand-int 4))
  (def emot (compute-emotion (word-split tweet)))

  (def parammap {"inst" instselect, "tweet" tweet, "emotion" emot })
  (play-sound parammap)
  )

;; since even 1% of twitter traffic is extreme we need to curtail
;; our music a bit, so we try to narrow it down to 100ms
(defn pick-and-play [tweet]
  (def curt (.getTime (new Date)))
  ; 160-100 = 60 spread, so 1 to 140 maps 0-60 + 100
  ;  (tweetlength/140)*60+100
  (def targetbpm (+ 100 (* 60 (/ tweetlength 140))))
  (if (> (- curt 100) prevt)
    (decided-to-play tweet))
  (def prevt curt)
  )

(def ^:dynamic *creds* (make-oauth-creds "DWQeZVQiFG1V0abuu5yKw"
                             "EXFHdOtIIBzzW6W4l5Aq8tx94pRUFv3pjHGLTqx8l4"
                         "562448092-aranxSAuTHi7BXcB9OMq25iq09qroEC4yEUieKwB"
                         "TyIFXRr5RclyfDeic7wMmnZmrpcqSElZWWeOkehKqOY"))


(defn decider-callback
        [response content]
        (def tweet (str content)); (:text (json/read-json (str content))))
        (def strcnt (count tweet))

        (pick-and-play tweet)
)


(defn -main [& args]
        (Thread/sleep 1000)
        (twp/twpkick)

        (statuses-sample
               :oauth-creds *creds*
               :callbacks (AsyncStreamingCallback. decider-callback (comp println response-return-everything)
                  exception-print))

        (Thread/sleep 60000)
        (stop)
        (System/exit 0)
)
