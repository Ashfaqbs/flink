################################################################################
#  Licensed to the Apache Software Foundation (ASF) under one
#  or more contributor license agreements.  See the NOTICE file
#  distributed with this work for additional information
#  regarding copyright ownership.  The ASF licenses this file
#  to you under the Apache License, Version 2.0 (the
#  "License"); you may not use this file except in compliance
#  with the License.  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
# limitations under the License.
################################################################################
from pyflink.common import Duration
from pyflink.datastream import (CheckpointingMode,
                                ExternalizedCheckpointRetention,
                                StreamExecutionEnvironment)
from pyflink.testing.test_case_utils import PyFlinkTestCase


class CheckpointConfigTests(PyFlinkTestCase):

    def setUp(self):
        self.env = StreamExecutionEnvironment\
            .get_execution_environment()

        self.checkpoint_config = self.env.get_checkpoint_config()

    def test_is_checkpointing_enabled(self):

        self.assertFalse(self.checkpoint_config.is_checkpointing_enabled())

        self.env.enable_checkpointing(1000)

        self.assertTrue(self.checkpoint_config.is_checkpointing_enabled())

    def test_get_set_checkpointing_mode(self):

        self.env.enable_checkpointing(1000)
        self.assertEqual(self.checkpoint_config.get_checkpointing_mode(),
                         CheckpointingMode.EXACTLY_ONCE)

        self.checkpoint_config.set_checkpointing_mode(CheckpointingMode.AT_LEAST_ONCE)

        self.assertEqual(self.checkpoint_config.get_checkpointing_mode(),
                         CheckpointingMode.AT_LEAST_ONCE)

        self.checkpoint_config.set_checkpointing_mode(CheckpointingMode.EXACTLY_ONCE)

        self.assertEqual(self.checkpoint_config.get_checkpointing_mode(),
                         CheckpointingMode.EXACTLY_ONCE)

    def test_get_set_checkpoint_interval(self):

        self.assertEqual(self.checkpoint_config.get_checkpoint_interval(), -1)

        self.checkpoint_config.set_checkpoint_interval(1000)

        self.assertEqual(self.checkpoint_config.get_checkpoint_interval(), 1000)

    def test_get_set_checkpoint_timeout(self):

        self.assertEqual(self.checkpoint_config.get_checkpoint_timeout(), 600000)

        self.checkpoint_config.set_checkpoint_timeout(300000)

        self.assertEqual(self.checkpoint_config.get_checkpoint_timeout(), 300000)

    def test_get_set_min_pause_between_checkpoints(self):

        self.assertEqual(self.checkpoint_config.get_min_pause_between_checkpoints(), 0)

        self.checkpoint_config.set_min_pause_between_checkpoints(100000)

        self.assertEqual(self.checkpoint_config.get_min_pause_between_checkpoints(), 100000)

    def test_get_set_max_concurrent_checkpoints(self):

        self.assertEqual(self.checkpoint_config.get_max_concurrent_checkpoints(), 1)

        self.checkpoint_config.set_max_concurrent_checkpoints(2)

        self.assertEqual(self.checkpoint_config.get_max_concurrent_checkpoints(), 2)

    def test_get_set_fail_on_checkpointing_errors(self):

        self.assertTrue(self.checkpoint_config.is_fail_on_checkpointing_errors())

        self.checkpoint_config.set_fail_on_checkpointing_errors(False)

        self.assertFalse(self.checkpoint_config.is_fail_on_checkpointing_errors())

    def test_get_set_tolerable_checkpoint_failure_number(self):

        self.assertEqual(self.checkpoint_config.get_tolerable_checkpoint_failure_number(), 0)

        self.checkpoint_config.set_tolerable_checkpoint_failure_number(2)

        self.assertEqual(self.checkpoint_config.get_tolerable_checkpoint_failure_number(), 2)

    def test_get_set_externalized_checkpoints_retention(self):

        self.assertFalse(self.checkpoint_config.is_externalized_checkpoints_enabled())

        self.assertEqual(self.checkpoint_config.get_externalized_checkpoint_retention(),
                         ExternalizedCheckpointRetention.NO_EXTERNALIZED_CHECKPOINTS)

        self.checkpoint_config.set_externalized_checkpoint_retention(
            ExternalizedCheckpointRetention.RETAIN_ON_CANCELLATION)

        self.assertTrue(self.checkpoint_config.is_externalized_checkpoints_enabled())

        self.assertEqual(self.checkpoint_config.get_externalized_checkpoint_retention(),
                         ExternalizedCheckpointRetention.RETAIN_ON_CANCELLATION)

        self.checkpoint_config.set_externalized_checkpoint_retention(
            ExternalizedCheckpointRetention.DELETE_ON_CANCELLATION)

        self.assertEqual(self.checkpoint_config.get_externalized_checkpoint_retention(),
                         ExternalizedCheckpointRetention.DELETE_ON_CANCELLATION)

    def test_is_unaligned_checkpointing_enabled(self):

        self.assertFalse(self.checkpoint_config.is_unaligned_checkpoints_enabled())
        self.assertFalse(self.checkpoint_config.is_force_unaligned_checkpoints())
        self.assertEqual(self.checkpoint_config.get_alignment_timeout(), Duration.of_millis(0))

        self.checkpoint_config.set_checkpoint_interval(10000)
        self.checkpoint_config.enable_unaligned_checkpoints()
        self.assertTrue(self.checkpoint_config.is_unaligned_checkpoints_enabled())

        self.checkpoint_config.disable_unaligned_checkpoints()
        self.assertFalse(self.checkpoint_config.is_unaligned_checkpoints_enabled())

        self.checkpoint_config.enable_unaligned_checkpoints(True)
        self.assertTrue(self.checkpoint_config.is_unaligned_checkpoints_enabled())

        self.checkpoint_config.set_force_unaligned_checkpoints(True)
        self.assertTrue(self.checkpoint_config.is_force_unaligned_checkpoints())

        self.checkpoint_config.set_alignment_timeout(Duration.of_minutes(1))
        self.assertEqual(self.checkpoint_config.get_alignment_timeout(), Duration.of_minutes(1))
