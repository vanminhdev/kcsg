using System;
using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata;

#nullable disable

namespace KcsWriteLog.Models
{
    public partial class KCS_DATAContext : DbContext
    {
        public KCS_DATAContext()
        {
        }

        public KCS_DATAContext(DbContextOptions<KCS_DATAContext> options)
            : base(options)
        {
        }

        public virtual DbSet<ActivityLog> ActivityLogs { get; set; }
        public virtual DbSet<ControllerIp> ControllerIps { get; set; }

        protected override void OnConfiguring(DbContextOptionsBuilder optionsBuilder)
        {

        }

        protected override void OnModelCreating(ModelBuilder modelBuilder)
        {
            modelBuilder.HasAnnotation("Relational:Collation", "Vietnamese_CI_AS");

            modelBuilder.Entity<ActivityLog>(entity =>
            {
                entity.ToTable("ActivityLog");

                entity.Property(e => e.EntryTime).HasColumnType("datetime");

                entity.Property(e => e.IpFrom)
                    .HasMaxLength(20)
                    .IsUnicode(false);

                entity.Property(e => e.IpUpdate)
                    .HasMaxLength(20)
                    .IsUnicode(false);

                entity.Property(e => e.TimeUpdate).HasColumnType("datetime");
            });

            modelBuilder.Entity<ControllerIp>(entity =>
            {
                entity.ToTable("ControllerIp");

                entity.Property(e => e.ControllerType)
                    .HasMaxLength(100)
                    .IsUnicode(false);

                entity.Property(e => e.IsActive).HasColumnName("isActive");

                entity.Property(e => e.Port)
                    .HasMaxLength(6)
                    .IsUnicode(false);

                entity.Property(e => e.RemoteIp)
                    .HasMaxLength(20)
                    .IsUnicode(false);
            });

            OnModelCreatingPartial(modelBuilder);
        }

        partial void OnModelCreatingPartial(ModelBuilder modelBuilder);
    }
}
