UPDATE audit_results SET change_status = 'IMPLEMENTATION_IN_PROGRESS' WHERE change_status = 'IN_PROGRESS';
UPDATE audit_results SET change_status = 'IMPLEMENTATION_COMPLETE' WHERE change_status = 'APPLIED';
UPDATE audit_results SET change_status = 'IMPLEMENTATION_FAILED' WHERE change_status = 'FAILED';