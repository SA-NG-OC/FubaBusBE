-- V14: Optimize booking queries performance
-- Add indexes for frequently queried columns to improve API performance

-- Index for booking status and search filters
CREATE INDEX IF NOT EXISTS idx_bookings_status ON bookings(bookingstatus);
CREATE INDEX IF NOT EXISTS idx_bookings_code_search ON bookings(bookingcode);
CREATE INDEX IF NOT EXISTS idx_bookings_customer_search ON bookings(customername, customerphone);
CREATE INDEX IF NOT EXISTS idx_bookings_created_at ON bookings(createdat DESC);

-- Index for booking-trip relationship (already have tripid FK, but add composite)
CREATE INDEX IF NOT EXISTS idx_bookings_trip_status ON bookings(tripid, bookingstatus);

-- Index for tickets by booking (foreign key relationship)
CREATE INDEX IF NOT EXISTS idx_tickets_booking ON tickets(bookingid);
CREATE INDEX IF NOT EXISTS idx_tickets_status ON tickets(ticketstatus);

-- Index for passengers by ticket (foreign key relationship)
CREATE INDEX IF NOT EXISTS idx_passengers_ticket ON passengers(ticketid);

-- Composite index for common booking queries
CREATE INDEX IF NOT EXISTS idx_bookings_status_created ON bookings(bookingstatus, createdat DESC);

-- Index for trip seats queries
CREATE INDEX IF NOT EXISTS idx_tripseats_trip ON tripseats(tripid);

COMMENT ON INDEX idx_bookings_status IS 'Optimize filtering by booking status';
COMMENT ON INDEX idx_bookings_code_search IS 'Optimize search by booking code';
COMMENT ON INDEX idx_bookings_customer_search IS 'Optimize search by customer name/phone';
COMMENT ON INDEX idx_bookings_created_at IS 'Optimize sorting by creation date';
COMMENT ON INDEX idx_tickets_booking IS 'Optimize ticket lookup by booking ID';
COMMENT ON INDEX idx_passengers_ticket IS 'Optimize passenger lookup by ticket ID';
