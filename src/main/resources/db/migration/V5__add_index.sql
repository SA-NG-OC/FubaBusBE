-- Index cho Booking (để tính doanh thu nhanh)
CREATE INDEX IF NOT EXISTS idx_booking_created_status ON bookings(createdAt, bookingStatus);

-- Index cho Ticket (để đếm vé nhanh)
CREATE INDEX IF NOT EXISTS idx_ticket_created_status ON tickets(createdAt, ticketStatus);

-- Index cho Trip (để lọc chuyến đi theo ngày)
CREATE INDEX IF NOT EXISTS idx_trip_departure_time ON trips(departureTime);

-- Index cho TripSeat (để đếm ghế đã đặt cực nhanh)
CREATE INDEX IF NOT EXISTS idx_tripseat_trip_status ON tripSeats(tripId, status);