import React from 'react'

const BasicApp = () => {
  const [data, setData] = React.useState(null)
  const [error, setError] = React.useState(null)
  
  React.useEffect(() => {
    fetch('http://localhost:8080/api/v1/demo/dataset/small')
      .then(res => {
        console.log('Response:', res)
        return res.json()
      })
      .then(data => {
        console.log('Data:', data)
        setData(data)
      })
      .catch(err => {
        console.error('Error:', err)
        setError(err.message)
      })
  }, [])
  
  return (
    <div style={{ padding: '20px', fontFamily: 'Arial' }}>
      <h1>基础测试页面</h1>
      <p>这是最简单的测试，如果你能看到这行字，说明React正常</p>
      
      <hr />
      
      <h2>API测试</h2>
      {error && <p style={{ color: 'red' }}>错误: {error}</p>}
      {data && (
        <div>
          <p style={{ color: 'green' }}>成功获取数据!</p>
          <pre>{JSON.stringify(data, null, 2).slice(0, 500)}...</pre>
        </div>
      )}
      {!data && !error && <p>加载中...</p>}
    </div>
  )
}

export default BasicApp