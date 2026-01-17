-- Bước 1: Xóa ràng buộc khóa ngoại hiện tại
ALTER TABLE public.trips
DROP CONSTRAINT trips_routeid_fkey;

-- Bước 2: Tạo lại ràng buộc với ON DELETE CASCADE
ALTER TABLE public.trips
ADD CONSTRAINT trips_routeid_fkey
FOREIGN KEY (routeid)
REFERENCES public.routes (routeid)
ON DELETE CASCADE;