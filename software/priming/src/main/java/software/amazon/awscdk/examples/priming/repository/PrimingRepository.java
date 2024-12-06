/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: MIT-0
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify,
 * merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package software.amazon.awscdk.examples.priming.repository;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import software.amazon.awscdk.examples.priming.model.Priming;

@Repository
public class PrimingRepository {

    private static final Logger log = LoggerFactory.getLogger(PrimingRepository.class);

    private final JdbcTemplate jdbcTemplate;

    public PrimingRepository(JdbcTemplate jdbcTemplate) {
        log.info("PrimingRepository->started");
        this.jdbcTemplate = jdbcTemplate;
        log.info("PrimingRepository->finished");
    }

    public List<Priming> findAll() {
        log.info("findAll->started");

        List<Priming> primings = jdbcTemplate.query("SELECT * FROM priming", (resultSet, rowNum) -> {
            return new Priming(UUID.fromString(resultSet.getString("id")), resultSet.getString("name"),
                    resultSet.getString("type"));
        });

        log.info("findAll->finished");

        return primings;
    }

}
