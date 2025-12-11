-- public."FinancialRules" definition

-- Drop table

-- DROP TABLE public."FinancialRules";

CREATE TABLE public."FinancialRules" (
	"Id" uuid NOT NULL,
	"UserId" uuid NOT NULL,
	"Type" text NOT NULL,
	"Category" varchar(100) NULL,
	"AmountLimit" numeric(18, 2) NOT NULL,
	"Period" text NOT NULL,
	"IsActive" bool NOT NULL,
	"CreatedAt" timestamptz NOT NULL,
	"UpdatedAt" timestamptz NULL,
	CONSTRAINT "PK_FinancialRules" PRIMARY KEY ("Id")
);
CREATE INDEX "IX_FinancialRules_UserId" ON public."FinancialRules" USING btree ("UserId");
CREATE INDEX "IX_FinancialRules_UserId_IsActive" ON public."FinancialRules" USING btree ("UserId", "IsActive");


-- public."FinancialRules" foreign keys

ALTER TABLE public."FinancialRules" ADD CONSTRAINT "FK_FinancialRules_Users_UserId" FOREIGN KEY ("UserId") REFERENCES public."Users"("Id") ON DELETE CASCADE;


-- public."RecurringTransactions" definition

-- Drop table

-- DROP TABLE public."RecurringTransactions";

CREATE TABLE public."RecurringTransactions" (
	"Id" uuid NOT NULL,
	"UserId" uuid NOT NULL,
	"Amount" numeric(18, 2) NOT NULL,
	"Type" text NOT NULL,
	"Category" varchar(100) NOT NULL,
	"Description" varchar(500) NULL,
	"Frequency" text NOT NULL,
	"StartDate" timestamptz NOT NULL,
	"EndDate" timestamptz NULL,
	"DayOfMonth" int4 NULL,
	"DayOfWeek" int4 NULL,
	"NextExecutionDate" timestamptz NOT NULL,
	"IsActive" bool NOT NULL,
	"CreatedAt" timestamptz NOT NULL,
	"UpdatedAt" timestamptz NULL,
	"IsPaidThisPeriod" bool DEFAULT false NOT NULL,
	"LastPaidDate" timestamptz NULL,
	CONSTRAINT "PK_RecurringTransactions" PRIMARY KEY ("Id")
);
CREATE INDEX "IX_RecurringTransactions_IsActive_NextExecutionDate" ON public."RecurringTransactions" USING btree ("IsActive", "NextExecutionDate");
CREATE INDEX "IX_RecurringTransactions_UserId" ON public."RecurringTransactions" USING btree ("UserId");


-- public."RecurringTransactions" foreign keys

ALTER TABLE public."RecurringTransactions" ADD CONSTRAINT "FK_RecurringTransactions_Users_UserId" FOREIGN KEY ("UserId") REFERENCES public."Users"("Id") ON DELETE CASCADE;

-- public."Transactions" definition

-- Drop table

-- DROP TABLE public."Transactions";

CREATE TABLE public."Transactions" (
	"Id" uuid NOT NULL,
	"UserId" uuid NOT NULL,
	"Amount" numeric(18, 2) NOT NULL,
	"Type" text NOT NULL,
	"Category" varchar(100) NOT NULL,
	"Date" timestamptz NOT NULL,
	"Source" text NOT NULL,
	"Description" varchar(500) NULL,
	"CreatedAt" timestamptz NOT NULL,
	"UpdatedAt" timestamptz NULL,
	CONSTRAINT "PK_Transactions" PRIMARY KEY ("Id")
);
CREATE INDEX "IX_Transactions_Date" ON public."Transactions" USING btree ("Date");
CREATE INDEX "IX_Transactions_UserId" ON public."Transactions" USING btree ("UserId");


-- public."Transactions" foreign keys

ALTER TABLE public."Transactions" ADD CONSTRAINT "FK_Transactions_Users_UserId" FOREIGN KEY ("UserId") REFERENCES public."Users"("Id") ON DELETE CASCADE;

