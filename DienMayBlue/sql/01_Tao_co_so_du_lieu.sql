/*
    ĐIỆN MÁY BLUE - FILE 01
    XÓA DATABASE CŨ VÀ TẠO LẠI DATABASE SẠCH

    CẢNH BÁO: File này xóa toàn bộ database DienMayBlue hiện tại.
    Chỉ chạy khi muốn dựng lại CSDL đúng theo lược đồ báo cáo.
*/
USE master;
GO

IF DB_ID(N'DienMayBlue') IS NOT NULL
BEGIN
    ALTER DATABASE DienMayBlue SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
    DROP DATABASE DienMayBlue;
END;
GO

CREATE DATABASE DienMayBlue;
GO

ALTER DATABASE DienMayBlue SET MULTI_USER;
GO

USE DienMayBlue;
GO

PRINT N'Đã tạo mới database DienMayBlue.';
GO
