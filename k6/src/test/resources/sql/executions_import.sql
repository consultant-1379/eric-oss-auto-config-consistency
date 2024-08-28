--
-- PostgreSQL database dump
--

-- Dumped from database version 13.13
-- Dumped by pg_dump version 13.13

-- Started on 2024-01-25 10:51:08 UTC

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- TOC entry 2994 (class 0 OID 16414)
-- Dependencies: 202
-- Data for Name: eacc_executions; Type: TABLE DATA; Schema: public; Owner: eacc-user
--

COPY public.eacc_executions (id, job_name, rule_set_name, scope_name, execution_type, execution_started_at, consistency_audit_started_at, consistency_audit_ended_at, execution_ended_at, execution_status, total_nodes_audited, total_nodes_failed, total_mos_audited, total_attributes_audited, inconsistencies_identified) FROM stdin;
195	dimensions	ruleset	merged_scope_dime	OPEN_LOOP	2024-01-25 10:48:00+00	2024-01-25 10:48:00+00	2024-01-25 10:49:15+00	2024-01-25 10:49:15+00	AUDIT_SUCCESSFUL	99	0	397	14805	7405
\.


--
-- TOC entry 3000 (class 0 OID 0)
-- Dependencies: 201
-- Name: eacc_executions_id_seq; Type: SEQUENCE SET; Schema: public; Owner: eacc-user
--

SELECT pg_catalog.setval('public.eacc_executions_id_seq', 195, true);


-- Completed on 2024-01-25 10:51:08 UTC

--
-- PostgreSQL database dump complete
--

