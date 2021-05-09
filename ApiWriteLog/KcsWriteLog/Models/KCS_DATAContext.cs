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
        public virtual DbSet<Config> Configs { get; set; }
        public virtual DbSet<ControllerIp> ControllerIps { get; set; }
        public virtual DbSet<DataTraining> DataTrainings { get; set; }
        public virtual DbSet<LogLatencyRead> LogLatencyReads { get; set; }
        public virtual DbSet<LogLatencyWrite> LogLatencyWrites { get; set; }
        public virtual DbSet<LogQlearningRatio> LogQlearningRatios { get; set; }
        public virtual DbSet<LogQlearningRead> LogQlearningReads { get; set; }
        public virtual DbSet<LogQlearningWrite> LogQlearningWrites { get; set; }
        public virtual DbSet<LogRead> LogReads { get; set; }
        public virtual DbSet<LogWrite> LogWrites { get; set; }
        public virtual DbSet<Qtable> Qtables { get; set; }
        public virtual DbSet<Temp> Temps { get; set; }
        public virtual DbSet<VersionDatum> VersionData { get; set; }

        protected override void OnConfiguring(DbContextOptionsBuilder optionsBuilder)
        {
            if (!optionsBuilder.IsConfigured)
            {
#warning To protect potentially sensitive information in your connection string, you should move it out of source code. You can avoid scaffolding the connection string by using the Name= syntax to read it from configuration - see https://go.microsoft.com/fwlink/?linkid=2131148. For more guidance on storing connection strings, see http://go.microsoft.com/fwlink/?LinkId=723263.
                optionsBuilder.UseSqlServer("Server=.\\SQLEXPRESS;Database=KCS_DATA;Integrated Security=True");
            }
        }

        protected override void OnModelCreating(ModelBuilder modelBuilder)
        {
            modelBuilder.HasAnnotation("Relational:Collation", "SQL_Latin1_General_CP1_CI_AS");

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

            modelBuilder.Entity<Config>(entity =>
            {
                entity.ToTable("Config");

                entity.Property(e => e.Time).HasColumnType("datetime");
            });

            modelBuilder.Entity<ControllerIp>(entity =>
            {
                entity.ToTable("ControllerIp");

                entity.Property(e => e.ControllerType)
                    .HasMaxLength(100)
                    .IsUnicode(false);

                entity.Property(e => e.Description)
                    .HasMaxLength(100)
                    .IsFixedLength(true);

                entity.Property(e => e.IsActive).HasColumnName("isActive");

                entity.Property(e => e.Port)
                    .HasMaxLength(6)
                    .IsUnicode(false);

                entity.Property(e => e.RemoteIp)
                    .HasMaxLength(20)
                    .IsUnicode(false);
            });

            modelBuilder.Entity<DataTraining>(entity =>
            {
                entity.ToTable("DataTraining");

                entity.Property(e => e.HostDst)
                    .HasMaxLength(5)
                    .IsUnicode(false);

                entity.Property(e => e.HostSrc)
                    .HasMaxLength(5)
                    .IsUnicode(false);

                entity.Property(e => e.Time).HasColumnType("datetime");

                entity.Property(e => e.TimeUpdate).HasColumnType("datetime");
            });

            modelBuilder.Entity<LogLatencyRead>(entity =>
            {
                entity.ToTable("LogLatencyRead");

                entity.Property(e => e.TimeRun).HasColumnType("datetime");
            });

            modelBuilder.Entity<LogLatencyWrite>(entity =>
            {
                entity.ToTable("LogLatencyWrite");

                entity.Property(e => e.TimeRun).HasColumnType("datetime");
            });

            modelBuilder.Entity<LogQlearningRatio>(entity =>
            {
                entity.ToTable("LogQLearningRatio");

                entity.Property(e => e.TimeRun).HasColumnType("datetime");
            });

            modelBuilder.Entity<LogQlearningRead>(entity =>
            {
                entity.ToTable("LogQLearningRead");

                entity.Property(e => e.TimeRun).HasColumnType("datetime");
            });

            modelBuilder.Entity<LogQlearningWrite>(entity =>
            {
                entity.ToTable("LogQLearningWrite");

                entity.Property(e => e.TimeRun).HasColumnType("datetime");
            });

            modelBuilder.Entity<LogRead>(entity =>
            {
                entity.ToTable("LogRead");

                entity.HasIndex(e => e.Start, "IX_LogRead_TimeStart");

                entity.Property(e => e.DstIp)
                    .IsRequired()
                    .HasMaxLength(20)
                    .IsUnicode(false);

                entity.Property(e => e.End).HasColumnType("datetime");

                entity.Property(e => e.LocalIp)
                    .IsRequired()
                    .HasMaxLength(20)
                    .IsUnicode(false);

                entity.Property(e => e.SrcIp)
                    .IsRequired()
                    .HasMaxLength(20)
                    .IsUnicode(false);

                entity.Property(e => e.Start).HasColumnType("datetime");
            });

            modelBuilder.Entity<LogWrite>(entity =>
            {
                entity.ToTable("LogWrite");

                entity.Property(e => e.DstIp)
                    .IsRequired()
                    .HasMaxLength(20)
                    .IsUnicode(false);

                entity.Property(e => e.End).HasColumnType("datetime");

                entity.Property(e => e.LocalIp)
                    .IsRequired()
                    .HasMaxLength(20)
                    .IsUnicode(false);

                entity.Property(e => e.SrcIp)
                    .IsRequired()
                    .HasMaxLength(20)
                    .IsUnicode(false);

                entity.Property(e => e.Start).HasColumnType("datetime");
            });

            modelBuilder.Entity<Qtable>(entity =>
            {
                entity.ToTable("QTable");

                entity.Property(e => e.Qvalues)
                    .IsRequired()
                    .HasColumnName("QValues");

                entity.Property(e => e.Rewards).IsRequired();
            });

            modelBuilder.Entity<Temp>(entity =>
            {
                entity.ToTable("Temp");

                entity.Property(e => e.Id).ValueGeneratedNever();

                entity.Property(e => e.TimeRun).HasColumnType("datetime");
            });

            modelBuilder.Entity<VersionDatum>(entity =>
            {
                entity.Property(e => e.Ip)
                    .IsRequired()
                    .HasMaxLength(20)
                    .IsUnicode(false);
            });

            OnModelCreatingPartial(modelBuilder);
        }

        partial void OnModelCreatingPartial(ModelBuilder modelBuilder);
    }
}
