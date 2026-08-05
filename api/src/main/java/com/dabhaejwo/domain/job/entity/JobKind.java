package com.dabhaejwo.domain.job.entity;

/** 작업 종류. 값은 {@code jobs.kind} 의 CHECK 제약과 일치해야 한다. */
public enum JobKind {
    CRAWL,
    RECRAWL,
    EMBED_DOC
}
