#!/usr/bin/env python2.6

'''
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
'''

import unittest
import doctest
from os.path import dirname, split, isdir



class TestAgent(unittest.TestSuite):
  def run(self, result):
    run = unittest.TestSuite.run
    run(self, result)
    return result

def all_tests_suite():
  suite = unittest.TestLoader().loadTestsFromNames([
    'TestHeartbeat',
    'TestHardware',
    'TestServerStatus',
    'TestFileUtil',
    'TestActionQueue',
    #'TestAmbariComponent',
    'TestAgentActions',
    'TestCertGeneration'
  ])
  return TestAgent([suite])

def main():
  parent_dir = lambda x: split(x)[0] if isdir(x) else split(dirname(x))[0]
  src_dir = os.getcwd()
  agent_dir = parent_dir(parent_dir(parent_dir(src_dir)))
  path = agent_dir + os.sep + "target/tests.log"
  file=open(path, "w")
  runner = unittest.TextTestRunner(stream=file)
  suite = all_tests_suite()
  raise SystemExit(not runner.run(suite).wasSuccessful())

if __name__ == '__main__':
  import os
  import sys
  import io
  sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))
  sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))) + os.sep + 'main' + os.sep + 'python')
  sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))) + os.sep + 'main' + os.sep + 'python' + os.sep + 'ambari_agent')
  main()