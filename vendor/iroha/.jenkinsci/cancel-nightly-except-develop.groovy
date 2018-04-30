#!/usr/bin/env groovy

def cancelThisJob() {
    def jobname = env.JOB_NAME
    def buildnum = env.BUILD_NUMBER.toInteger()
    def job = Jenkins.instance.getItemByFullName(jobname)

    if (jobname =~ /^.*\/${job.name}$/) {
        for (build in job.builds) {
            if (!build.isBuilding()) { continue; }
            if (buildnum == build.getNumber().toInteger()) { build.doStop(); }
        }
    }
}
return this
