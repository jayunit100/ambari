/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

App.dataSetMapper = App.QuickDataMapper.create({
  model: App.DataSet,
  Jobs_model: App.DataSetJob,
  config: {
    id: 'id', // approach 2 : to be calculated (TBC1)
    name: 'Feeds.name', // from json
    source_cluster_name: 'Feeds.clusters.cluster[0].name', // approach1 : from json
    target_cluster_name: 'target_cluster_name', // approach 2 : to be calculated (TBC2)
    source_dir: 'Feeds.locations.location.path',
    schedule: 'Feeds.frequency',
    dataset_jobs: 'dataset_jobs', // TBC3 ( set of ids will be added )

    // all below are unknown at present and may be blank
    last_failed_date: 'last_failed_date', // TBC4
    last_succeeded_date: 'last_succeeded_date', // TBC5
    last_duration: 'last_duration', // TBC6
    avg_data: 'avg_data', // TBC7
    created_date: 'created_date', // TBC8
    target_dir: 'target_dir'

  },
  jobs_config: {
    $dataset_id: 'none', // will be loaded outside parser
    id: 'Instances.id',
    start_date: 'start_date_str',
    end_date: 'end_date_str',
    duration: 'duration'
    //data: 'Instances.details'
  },

  map: function (json) {
    if (!this.get('model')) {
      return;
    }
    if (json && json.items && json.items.length > 0) {
      var dataset_results = [];
      json.items.forEach(function (item) {

        try {
          // TBC1
          var re = new RegExp(" ", "g");
          item.id = item.Feeds.name.replace(re, "_");

          // TBC2
          item.target_cluster_name = (item.Feeds.clusters.cluster.findProperty("type", "target")).name;

          // TBC3
          item.dataset_jobs = [];

          var last_failed_date = null;
          var last_succeeded_date = null;
          var last_end_date = null;
          item.instances.forEach(function (job) {
            var end_date = new Date(job.Instances.end);

            if (!last_end_date) {
              last_end_date = end_date;
              item.last_job = job;
            }
            else if (end_date > last_end_date) {
              last_end_date = end_date;
              item.last_job = job;
            }
            if (job.Instances.status === 'FAILED') {
              if (last_failed_date == null || last_failed_date < end_date) {
                item.last_failed_date = end_date.getTime();
                last_failed_date = end_date;
              }
            }
            else if (job.Instances.status === 'SUCCESSFUL') {
              if (last_succeeded_date == null || last_succeeded_date < end_date) {
                item.last_succeeded_date = end_date.getTime();
                last_succeeded_date = end_date;
              }
            }

            item.dataset_jobs.push(job.Instances.id);
          });

          // calculate last_duration

          var last_end_date = new Date(item.last_job.Instances.end);
          var last_start_date = new Date(item.last_job.Instances.start);
          item.last_duration = last_end_date - last_start_date;


          item.avg_data = '';
          item.created_date = '';
          item.target_dir = '';

          var newitem = this.parseIt(item, this.config);
          dataset_results.push(newitem);
        } catch (ex) {
          console.debug('Exception occured : ' + ex);
        }
      }, this);
      console.debug('Before load: App.DataSet.find().content : ' + App.DataSet.find().content);
      App.store.loadMany(this.get('model'), dataset_results);
      console.debug('After load: App.DataSet.find().content : ' + App.DataSet.find().content);

      try {
        // Child records
        var dataset_job_results = [];
        json.items.forEach(function (item) {
          item.instances.forEach(function (instance) {
            instance.Instances.start = new Date(instance.Instances.start); // neeed to be calulated end -start
            instance.Instances.end = new Date(instance.Instances.end); // neeed to be calulated end -start


            instance.duration = instance.Instances.end - instance.Instances.start;

            instance.start_date_str = instance.Instances.start.toString();
            instance.end_date_str = instance.Instances.end.toString();

            var result = this.parseIt(instance, this.jobs_config);
            result.dataset_id = item.id;
            dataset_job_results.push(result);


          }, this)
        }, this);

        console.debug('Before load: App.DataSetJob.find().content : ' + App.DataSetJob.find().content);
        App.store.loadMany(this.get('Jobs_model'), dataset_job_results);
        console.debug('After load: App.DataSetJob.find().content : ' + App.DataSetJob.find().content);
      } catch (ex) {
        console.debug('Exception occured : ' + ex);
      }
    }
  }

});
