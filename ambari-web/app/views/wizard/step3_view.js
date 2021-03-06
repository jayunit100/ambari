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


var App = require('app');

App.WizardStep3View = Em.View.extend({

  templateName: require('templates/wizard/step3'),
  category: '',

  didInsertElement: function () {
    this.get('controller').navigateStep();
  }
});

//todo: move it inside WizardStep3View
App.WizardHostView = Em.View.extend({

  tagName: 'tr',
  classNameBindings: ['hostInfo.bootStatus'],
  hostInfo: null,

  remove: function () {
    this.get('controller').removeHost(this.get('hostInfo'));
  },

  retry: function() {
    this.get('controller').retryHost(this.get('hostInfo'));
  },

  isRemovable: function () {
    return true;
  }.property(),

  isRetryable: function() {
    // return ['FAILED'].contains(this.get('hostInfo.bootStatus'));
    return false;
  }.property('hostInfo.bootStatus')

});


