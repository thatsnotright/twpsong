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
   [clojure.stacktrace]
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

(def ^:dynamic pianodelay 220)

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

(def ^:dynamic *dubstepid* (ref 0))
(defn get-dub-id [] @*dubstepid*)
(defn set-dub-id [val] (dosync (ref-set *dubstepid* val)))
(def initial_i (:target (dubstep)))
(set-dub-id initial_i)

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
   (ctl #(get-dub-id) :wobble wrate)
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

(def ^:dynamic *prevt* (ref 0))
(defn get-prevt [] @*prevt*)
(defn set-prevt [val]
  (dosync (ref-set *prevt* val)))
(set-prevt 0)
; last time we played a piano melody
(def last_piano 0)
; last time we played a base drum
(def last_kick 0)
(def drop_on false)


(def ^:dynamic *tot-bpm* (ref 120))
(defn get-total-bpm [] @*tot-bpm*)
(defn set-total-bpm [newbpm]
  (dosync (ref-set *tot-bpm* newbpm))
  )
                                        ; current bpm derived from average tweet length normalized

                                        ; 140 bytes maps to 100-160 BPM
(def ^:dynamic *dbpm* (ref {}))
(defn get-bucket-ctr [key]
  (def cnt 1)
  (if (contains? @*dbpm* key)
    (def cnt (get @*dbpm* key))
    )
  cnt
  )
(defn add-bucket-ctr [key val]
  (dosync (ref-set *dbpm* (merge-with + @*dbpm* {key val} ))))
(defn set-bucket-ctr [key val]
  (dosync (ref-set *dbpm* (merge @*dbpm* {key val}))))
; track the bpm of each instrument bucket
(def ^:dynamic *bpm-map* (ref {}))
(defn set-bpm [key val]
  (dosync (ref-set *bpm-map* (merge @*bpm-map* {key val})))
  )
(defn get-cur-bpm [key]
  (if (contains? @*bpm-map* key) (get @*bpm-map* key) (get-total-bpm) )
  )

; track the last time an instrument was played by instrument
(def ^:dynamic *inst-last-beat* (ref {}))
(defn get-last-beat [key]

  (if (contains? @*inst-last-beat* key)
    (get @*inst-last-beat* key) 0)
  )
(defn set-last-beat [key val]
  (dosync (ref-set *inst-last-beat* (merge @*inst-last-beat* {key val})))
  )
; track when we last updated stats
(def ^:dynamic *hist-up-time* (ref (.getTime (new Date))))
(defn get-hist-time [] @*hist-up-time*)
(defn set-hist-time [newtime]
  (dosync (ref-set *hist-up-time* newtime))
  )
; track the time at which we started seeing tweets
(def ^:dynamic *rate_start* (ref (.getTime (new Date))))
(defn get-rate-start [] @*rate_start*)
(defn set-rate-start [val]
  (dosync (ref-set *rate_start* val)))
; track how many total tweets we see
(def ^:dynamic *tweetcount* (ref 0))
(defn get-tweet-count [] @*tweetcount*)
(defn set-tweet-count [val]
  (dosync (ref-set *tweetcount* val)))

(defn update-histogram-bpm [curhtime]
                                        ; loop through all the buckets in our histogram, these are
                                        ; instrument play counts which gives us
                                        ; a histogram of the current twitter traffic
                                        ; then take this count, divide it by the number of minutes
                                        ; that is tweets per minute
                                        ; divide this by 20 to scale for estimated traffic
                                        ; this becomes the bpm of this bucket
                                        ; and reset the stats
      (doseq [keyval @*dbpm*]
        (do
          (def tkey (nth keyval 0))
          (def twecnt (nth keyval 1))
          (def berate (/ twecnt (get-tweet-count)))
          (def buck-bpm (* (get-total-bpm) berate))
          (set-bpm key buck-bpm)
          (prn "bucket " (str tkey) " bpm " (str buck-bpm) " tweet cnt " twecnt "total" (get-tweet-count))
          (set-bucket-ctr tkey 0)
          )
        )
      (set-tweet-count 0)
      (set-hist-time curhtime)
      )

(defn decided-to-play [tweet]
  (def cnt (count tweet))
  (def bucket (- cnt (mod cnt 5) ))
  (if (contains? synthz bucket)
    (do
    (def curtime (.getTime (new Date)))
    (def elapsedt (- curtime (get-last-beat bucket)))
    ; assume everything is a quarter note
    (if (> elapsedt (* (/ 60 (get-cur-bpm bucket) ) 10000))
      (do
        ( (get synthz bucket))
        (set-last-beat bucket curtime)
        )

      )
    )
    )

    (add-bucket-ctr bucket 1)
                                        ; update stats for this bucket so we can keep a running histogram
  )


;; since even 1% of twitter traffic is extreme we need to curtail
;; our music a bit, so we try to narrow it down to 100ms

;; we also decide to alter the BPM to do a "drop" if it's been at least 10 seconds
;; since the last drop and we find a tweet with drop in it
(defn pick-and-play [tweet]
  (def curt (.getTime (new Date)))
  (def tweettext (:text (json/read-json (str tweet))))
  (if (> (- curt 10000) (get-prevt))
    (do
     (if (nil? tweettext)
       (doseq [n [96 100 103]] (piano n))
       (callpiano tweettext)
     )
     (set-prevt curt)
     )
    )

  (if (nil? tweettext) (def tweettext ""))
  (decided-to-play tweettext)

)



(def ^:dynamic *creds* (make-oauth-creds "DWQeZVQiFG1V0abuu5yKw"
                                         "EXFHdOtIIBzzW6W4l5Aq8tx94pRUFv3pjHGLTqx8l4"
                                         "562448092-aranxSAuTHi7BXcB9OMq25iq09qroEC4yEUieKwB"
                                         "TyIFXRr5RclyfDeic7wMmnZmrpcqSElZWWeOkehKqOY"))

(defn decider-callback
  [response content]
  (def tweet (str content)); (:text (json/read-json (str content))))
  (def strcnt (count tweet))
  (def curt (.getTime (new Date)))
  (def elapsed (- curt (get-rate-start)))
  (set-tweet-count (+ 1 (get-tweet-count)))
  (if (> elapsed 6000)
    (do
      ; calculate the tweets per minute
      (def tpm (/ (get-tweet-count) (/ (/ elapsed 1000) 60)))
      ; seems we get about 3100 tweets per minute
      ; so let's just do a simple scale and divide by a factor
      (def beatrate (/ tpm 10.0))
      (ctl @*dubstepid* :wobble beatrate)
      (set-total-bpm beatrate)
      (prn "bpm " beatrate)
      (prn @*bpm-map*)
      (update-histogram-bpm curt)
      (set-rate-start curt)
      (set-tweet-count 0)
      )
  )
  (try (pick-and-play tweet) (catch Exception e (print-stack-trace e)))
    ; update the histogram

  )


(defn -main [& args]
  (twp/twpkick)

  (statuses-sample
   :oauth-creds *creds*
   :callbacks (AsyncStreamingCallback. decider-callback (comp println response-return-everything)
                                       exception-print))

  (Thread/sleep 360000)
  (stop)
  (System/exit 0)
  )
