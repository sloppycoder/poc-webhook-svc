--test data

insert into webhook_requests
    (message_id, client_id, message_type, payload, status)
values
    ('11-22-33-44', 'CL001', 'CREDIT_ADVICE', '{"amount":100.00}', 'NEW'),
    ('11-22-33-45', 'CL002', 'DEBIT_ADVICE', '{"amount":-101.00}', 'NEW')
;