# 02 - Data Queries

## 1. Sum value of "Number of days with maximum 8-hour average ozone concentration over the National Ambient Air Quality Standard" per year

```sql
with wantedmeasure(measureid) as (
    select measureid
    from public.measures m
    where measurename = 'Number of days with maximum 8-hour average ozone concentration over the National Ambient Air Quality Standard'
)
select
    reportyear
     , sum(value) as total
from public.air_quality_reports aqr
where measureid in (select * from wantedmeasure)
group by reportyear
```

## 2. Year with max value of "Number of days with maximum 8-hour average ozone concentration over the National Ambient Air Quality Standard" from year 2008 and later (inclusive)"

```sql
with wantedmeasure(measureid) as (
    select measureid
    from public.measures m
    where measurename = 'Number of days with maximum 8-hour average ozone concentration over the National Ambient Air Quality Standard'
)
select x.reportyear
from (
         select
             reportyear
              , value
              , rank() over(partition by measureid order by value desc) as rnk
         from public.air_quality_reports aqr
         where measureid in (select * from wantedmeasure)
           and reportyear::int >= 2008
     ) as x
where x.rnk = 1
```

## 3. Max value of each measurement per state

```sql
select
    aqr.measureid
    , m.measurename
    , aqr.statename
    , max(aqr.value) as max_value
from public.air_quality_reports aqr
         join public.measures m
              on aqr.measureid = m.measureid
group by 1,2,3
```

## 4. Average value of "Number of person-days with PM2.5 over the National Ambient Air Quality Standard (monitor and modeled data)" per year and state in ascending order

```sql
with wantedmeasure(measureid) as (
    select measureid
    from public.measures m
    where measurename = 'Number of person-days with PM2.5 over the National Ambient Air Quality Standard (monitor and modeled data)'
)
select
       reportyear
     , statename
     , avg(value)::int as average
from public.air_quality_reports
where measureid in (select * from wantedmeasure)
-- measurements with value equal to 0 are probably bad data
-- especially when previous year values were several orders of magnitude bigger
and value != 0
group by reportyear, statename
order by reportyear, statename
```

## 5. State with the max accumulated value of "Number of days with maximum 8-hour average ozone concentration over the National Ambient Air Quality Standard" overall years

```sql
with wantedmeasure(measureid) as (
    select measureid
    from public.measures m
    where measurename = 'Number of days with maximum 8-hour average ozone concentration over the National Ambient Air Quality Standard'
), yearly_values (reportyear, statename, yearly_value) as (
    select
        reportyear
         , statename
         , sum(value) as yearly_value
    from public.air_quality_reports aqr
    where measureid in (select * from wantedmeasure)
    group by 1,2
    order by statename, reportyear
)
select
    x.statename
     , x.yearly_cumsum
from (
         select
             reportyear
              , statename
              , sum(yearly_value) over(partition by statename order by reportyear) as yearly_cumsum
         from yearly_values
     ) as x
order by x.yearly_cumsum desc
limit 1
```

## 6. Average value of "Number of person-days with maximum 8-hour average ozone concentration over the National Ambient Air Quality Standard" in the state of Florida

```sql
with wantedmeasure(measureid) as (
    select measureid
    from public.measures m
    where measurename = 'Number of person-days with maximum 8-hour average ozone concentration over the National Ambient Air Quality Standard'
), state_county_averages(countyname, average_value) as (
    -- to calculate the average of the state, I am first calculating the average over the years for each county
    select countyname, avg(value) as average_value
    from public.air_quality_reports aqr
    where measureid in (select * from wantedmeasure)
      and statename = 'Florida'
      -- there are measurements with value equal to 0 which are bad measurements
      and value <> 0
    group by countyname
)
select 'Florida' as statename, avg(average_value) as average_value
from state_county_averages
```

## 7. County with min "Number of days with maximum 8-hour average ozone concentration over the National Ambient Air Quality Standard" per state per year

```sql
with wantedmeasure(measureid) as (
    select measureid
    from public.measures m
    where measurename = 'Number of days with maximum 8-hour average ozone concentration over the National Ambient Air Quality Standard'
)
select
    x.reportyear
     , x.statename
     , x.value
     -- there are ties in the ranking, to avoid losing any information, I am carrying over all the counties
     , array_agg(x.countyname) as countynames
from (
         select
             reportyear
              , statename
              , countyname
              , value
              , rank() over(partition by reportyear, statename order by value) as rnk
         from public.air_quality_reports aqr
         where measureid in (select * from wantedmeasure)
     ) as x
where x.rnk = 1
group by 1,2,3
order by 1,2
```