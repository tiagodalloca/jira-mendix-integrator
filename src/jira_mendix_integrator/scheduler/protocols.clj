(ns jira-mendix-integrator.scheduler.protocols)

(defprotocol Scheduler
  (add-job [this job-id scheduled-job])
  (remove-job [this job-id])
  (get-job [this job-id]))

(defprotocol ScheduledJob
  (start [this])
  (stop [this]))
