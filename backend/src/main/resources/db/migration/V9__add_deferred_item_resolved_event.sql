ALTER TABLE deferred_item
    ADD COLUMN resolved_event_id UUID,
    ADD CONSTRAINT fk_deferred_item_event FOREIGN KEY (resolved_event_id) REFERENCES event(id);
