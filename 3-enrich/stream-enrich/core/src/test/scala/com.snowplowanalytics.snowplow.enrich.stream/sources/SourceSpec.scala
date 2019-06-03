/*
 * Copyright (c) 2013-2019 Snowplow Analytics Ltd.
 * All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0, and
 * you may not use this file except in compliance with the Apache License
 * Version 2.0.  You may obtain a copy of the Apache License Version 2.0 at
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License Version 2.0 is distributed on an "AS
 * IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the Apache License Version 2.0 for the specific language
 * governing permissions and limitations there under.
 */
package com.snowplowanalytics.snowplow.enrich.stream
package sources

import java.time.Instant

import org.specs2.mutable.Specification
import com.snowplowanalytics.iglu.core.SelfDescribingData
import com.snowplowanalytics.snowplow.enrich.common.outputs._

class SourceSpec extends Specification {

  "getSize" should {
    "get the size of a string of ASCII characters" in {
      Source.getSize("abcdefg") must_== 7
    }

    "get the size of a string containing non-ASCII characters" in {
      Source.getSize("™®字") must_== 8
    }
  }

  "adjustOversizedFailureJson" should {
    "truncate the original bad row" in {
      val original = SelfDescribingData[BadRow](
        Source.oversizedBadRow,
        BadRow(
          CPFormatViolation(
            Instant.ofEpochSecond(12),
            "tsv",
            FallbackCPFormatViolationMessage("ah")
          ),
          RawPayload("ah"),
          Processor.default
        )
      )
      val res = Source.adjustOversizedFailureJson(original, 200, Processor.default)
      res.schema must_== Source.oversizedBadRow
      res.data.failure must haveClass[SizeViolation]
      val f = res.data.failure.asInstanceOf[SizeViolation]
      f.actualSizeBytes must_== 404
      f.maximumAllowedSizeBytes must_== 200
      f.expectation must_== "bad row exceeded the maximum size"
      res.data.payload must_== RawPayload("""{"schema":"iglu:com.snowplowanalytics.sn""")
      res.data.processor must_== Processor.default
    }
  }

  "oversizedSuccessToFailure" should {
    "create a bad row JSON from an oversized success" in {
      val res = Source.oversizedSuccessToFailure("abcdefghijklmnopqrstuvwxy", 10, Processor.default)
      res.schema must_== Source.oversizedBadRow
      res.data.failure must haveClass[SizeViolation]
      val f = res.data.failure.asInstanceOf[SizeViolation]
      f.actualSizeBytes must_== 25
      f.maximumAllowedSizeBytes must_== 10
      f.expectation must_== "event passed enrichment but exceeded the maximum allowed size as a result"
      res.data.payload must_== RawPayload("ab")
      res.data.processor must_== Processor.default
    }
  }
}
